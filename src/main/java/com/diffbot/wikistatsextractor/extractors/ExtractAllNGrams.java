package com.diffbot.wikistatsextractor.extractors;

import com.diffbot.wikistatsextractor.dumpparser.DumpParser;
import com.diffbot.wikistatsextractor.util.Util;
import opennlp.tools.util.Span;
import org.dbpedia.spotlight.db.model.StringTokenizer;
import org.dbpedia.spotlight.db.tokenize.TextTokenizerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.diffbot.wikistatsextractor.extractors.ExtractSFAndRedirections.NB_WORKERS;

/**
 * Another important part of spotlight, produce a file, 
 * sfAndTotalCount that contains for each surface form the number of time
 * this SF is used as a surface form vs the number of time it appears in 
 * the text.
 * @author sam
 *
 */
public class ExtractAllNGrams {
	/** max nb token that a surface form can have */
	public static int MAX_NB_TOKEN = 6;

	public static class AllNGramsWorker extends DumpParser.Worker {
		ConcurrentHashMap<String, AtomicInteger> all_interesting_sf;

		protected StringTokenizer spotlightTokenizer;

		public AllNGramsWorker(ConcurrentHashMap<String, AtomicInteger> all_interesting_sf, StringTokenizer spotlightTokenizer) {
			this.all_interesting_sf = all_interesting_sf;
			this.spotlightTokenizer = spotlightTokenizer;
		}

		@Override
		public void doSomethingWithPage(String page) {
			List<String> paragraphs = Util.getCleanTextFromPage(page, true, true, true,true);

			if (paragraphs == null)
				return;

			for (String paragraph : paragraphs) {
				Span[] spans = spotlightTokenizer.tokenizePos(paragraph);
				for (int i = 0; i < spans.length; i++) {
					for (int j = 0; j < MAX_NB_TOKEN-1; j++) {
						if (i+j < spans.length) {
							String substr = paragraph.substring(spans[i].getStart(), spans[i+j].getEnd());
							AtomicInteger count = all_interesting_sf.get(substr);
							if (count != null) count.incrementAndGet();
						}
					}
				}
			}

		}
	}

	public static void extractAllNGrams(String path_to_wiki_dump, String path_to_surface_form_file, String path_to_output,
			TextTokenizerFactory spotlightTokenizerFactory) {
		/** get all the surface forms */
		ConcurrentHashMap<String, AtomicInteger> surface_forms=null;
		/** also keep in mind the surface forms that are all in lower case, for later */
		HashSet<String> lowercase_surface_forms=new HashSet<String>();
		try {
			surface_forms = new ConcurrentHashMap<String, AtomicInteger>(5000000,0.5f,7);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path_to_surface_form_file)), "UTF8"), 16 * 1024);
			String line = br.readLine();
			while (line != null) {
				String[] split = Util.fastSplit(line);
				if (split.length != 2) {
					line = br.readLine();
					continue;
				}
				String sf = split[0];
				String lower_case = sf.toLowerCase();
				surface_forms.put(sf, new AtomicInteger(0));
				if(sf.equals(lower_case)) lowercase_surface_forms.add(sf);
				line=br.readLine();
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println(surface_forms.size() + " elements in the interesting sf");

		/** launch the dump Parsing */
		System.out.println("Starting NGram workers");
		DumpParser dp = new DumpParser();
		for (int i = 0; i < NB_WORKERS; i++)
			dp.addWorker(new AllNGramsWorker(surface_forms, spotlightTokenizerFactory.createTokenizer().getStringTokenizer()));
		dp.extract(path_to_wiki_dump);

		/** creates the file we want */
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path_to_surface_form_file)), "UTF8"), 16 * 1024);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path_to_output)), "UTF8"));
			String line=br.readLine();
			while(line!=null){
				String[] split=Util.fastSplit(line);
				if (split.length==2){
					String sf=split[0];
					bw.write(sf+"\t"+split[1]+"\t"+surface_forms.get(sf).get()+'\n');
					if (!sf.equals(sf.toLowerCase()) && !lowercase_surface_forms.contains(sf.toLowerCase())){
						bw.write(sf.toLowerCase()+"\t-1\t"+surface_forms.get(sf).get()+'\n');
					}
					
				}
				line=br.readLine();
			}
			br.close();
			bw.close();
		} catch (IOException ioe) {
		}

		System.out.println("Finished writing sfAndTotalCounts");

	}

}
