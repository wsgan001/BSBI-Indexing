/**
 * Index.java
 * Project 1
 * YouGle: Your First Search Engine
 * Created by 
 * 1. Peerachai  Banyongrakkul  Sec.1  5988070
 * 2. Sakunrat  Nunthavanich  Sec.1  5988095
 * 3. Boonyada  Lojanarungsiri  Sec.1  5988153
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the given file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws IOException {
		/*
		 * TODO: Your code here
		 * 
		 */
		index.writePosting(fc, posting);
		 
	}
	

	 /**
     * Pop next element if there is one, otherwise return null
     * @param iter an iterator that contains integers
     * @return next element or null
     */
    private static Integer popNextOrNull(Iterator<Integer> iter) {
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }
	
    
   
	/**
	 * Main method to start the indexing process.
	 * @param method		:Indexing method. "Basic" by default, but extra credit will be given for those
	 * 			who can implement variable byte (VB) or Gamma index compression algorithm
	 * @param dataDirname	:relative path to the dataset root directory. E.g. "./datasets/small"
	 * @param outputDirname	:relative path to the output directory to store index. You must not assume
	 * 			that this directory exist. If it does, you must clear out the content before indexing.
	 */
	public static int runIndexer(String method, String dataDirname, String outputDirname) throws IOException 
	{
		/* Get index */
		String className = method + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}
		
		/* Get root directory */
		File rootdir = new File(dataDirname);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + dataDirname);
			return -1;
		}
		
		   
		/* Get output directory*/
		File outdir = new File(outputDirname);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + outputDirname);
			return -1;
		}
		
		/*	TODO: delete all the files/sub folder under outdir
		 * 
		 */
		if(outdir.isDirectory())
		{
			if(outdir.list().length>0)
			{
				deleteSub(outdir);
			}
			else
			{
				System.err.println("Output directory is already empty.");
			}
		}
		
		
		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return -1;
			}
		}
		
		
		
		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();
		PostingList post;

		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(outputDirname, block.getName());
			System.out.println("Processing block "+block.getName());
			blockQueue.add(blockFile);

			File blockDir = new File(dataDirname, block.getName());
			File[] filelist = blockDir.listFiles();
			
			//create new posList after changing block
			Map<Integer,PostingList> blockPost = new TreeMap<Integer,PostingList>();
			
			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				
				 // use pre-increment to ensure docID > 0
                int docId = ++docIdCounter;
                docDict.put(fileName, docId);
				
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split( "\\s+");		// \\s+ is one or more whitespace
					for (String token : tokens) {
						/*
						 * TODO: Your code here
						 *       For each term, build up a list of
						 *       documents in which the term occurs
						 */
						ArrayList<Integer> eachPost = new ArrayList<Integer>();
						if(termDict.isEmpty())
						{
							wordIdCounter++;
							termDict.put(token,wordIdCounter);
							eachPost.add(docId);
							post = new PostingList(wordIdCounter,eachPost);
							blockPost.put(wordIdCounter, post);
						}
						else
						{
							if(!termDict.containsKey(token))
							{
								wordIdCounter++;
								termDict.put(token, wordIdCounter);
							}
							int tid = termDict.get(token);
							if(blockPost.containsKey(tid))
							{
								if(!blockPost.get(tid).getList().contains(docId))
								{
									blockPost.get(tid).getList().add(docId);
								}
							}
							else
							{
								eachPost.add(docId);
								post = new PostingList(tid,eachPost);
								blockPost.put(tid, post);
							}
						}
					}
				}
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return -1;
			}
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			FileChannel inChannel = bfc.getChannel();
			/*
			 * TODO: Your code here
			 *       Write all posting lists for all terms to file (bfc) 
			 */
			// sort
			for (Integer termId : blockPost.keySet()) {
				writePosting(inChannel, blockPost.get(termId));
			}
			
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println("Total Files Indexed: "+totalFileCount);

		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			
			File combfile = new File(outputDirname, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return -1;
			}

			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/*
			 * TODO: Your code here
			 *       Combine blocks bf1 and bf2 into our combined file, mf
			 *       You will want to consider in what order to merge
			 *       the two blocks (based on term ID, perhaps?).
			 *       
			 */
			System.out.println("Merge Block ("+b1.getName() + ", " + b2.getName() + ")");
			
			mergeBlock( bf1,  bf2,  mf);
			
			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			blockQueue.add(combfile);
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(outputDirname, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				outputDirname, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				outputDirname, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				outputDirname, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
		
		return totalFileCount;
	}
	
	/**
	 * Delete files and sub-folders
	 * @param file
	 * @throws IOException
	 */
	private static void deleteSub(File file) throws IOException
	{
		System.out.println("Delete all files and sub-folders in " + file.getName());
		for (File sub : file.listFiles()) 
		{
			 
			if (sub.isDirectory()) 
			{
				deleteSub(sub);
			} 
			else 
			{
				if (!sub.delete()) 
				{
					throw new IOException();
				}
			}
			if (sub.isFile()) 
			{
				deleteSub(sub);
			} 
		}
 
		if (!file.delete()) 
		{
			throw new IOException();
		}
	}
	
	/**
	 * 
	 * @param f1
	 * @param f2
	 * @param combFile
	 * @throws IOException
	 */
	private static void mergeBlock(RandomAccessFile f1, RandomAccessFile f2, RandomAccessFile combFile) throws IOException
	{
		FileChannel block1 = f1.getChannel();
		FileChannel block2 = f2.getChannel();
		FileChannel combBlock = combFile.getChannel();
		
		while(true)
		{
			PostingList post1 = index.readPosting(block1);
			PostingList post2 = index.readPosting(block2);
        	PostingList newPost = null;
			if(post1 == null && post2 == null)
			{
				break;
			}
			else
			{
				while (post1 != null) 
				{
					if(post2 == null || post1.getTermId() < post2.getTermId())
					{
	        			if(blockQueue.size() <= 0)
	        			{
	        				writePostDict(post1.getTermId(), combBlock.position(), post1.getList().size());
	        			}
	                    writePosting(combBlock, post1);
	                    post1 = index.readPosting(block1);
					}
					else
					{
						break;
					}
                } 
                while (post2 != null) 
                {
                	if(post1 == null || post2.getTermId() < post1.getTermId())
                	{
	        			if(blockQueue.size() <= 0)
	        			{
	        				writePostDict(post2.getTermId(), combBlock.position(), post2.getList().size());
	        			}
	                    writePosting(combBlock, post2);
	                    post2 = index.readPosting(block2);
                	}
                	else
                	{
                		break;
                	}
                }
                if (post1 != null && post2 != null && post1.getTermId() == post2.getTermId()) 
                {
                    newPost = mergePosting(post1, post2);
        			if(blockQueue.size() <= 0)
        			{
        				writePostDict(newPost.getTermId(), combBlock.position(), newPost.getList().size());
        			}
                    writePosting(combBlock, newPost);
                }
            }
        } 
	}
	
    /**
     * 
     * @param termId
     * @param pos
     * @param docFreq
     */
    private static void writePostDict(int termId, long pos, int docFreq)
    {
		Pair<Long,Integer> pairDoc;
		pairDoc = new Pair<>(pos, docFreq);
		postingDict.put(termId,pairDoc);
    }
	
    private static PostingList mergePosting(PostingList p1, PostingList p2) 
    {
        int termID;
        Iterator<Integer> docList1 = p1.getList().iterator();
        Iterator<Integer> docList2 = p2.getList().iterator();
        List<Integer> newDocList = new ArrayList<Integer>();
        Integer docID1 = popNextOrNull(docList1);
        Integer docID2 = popNextOrNull(docList2);
        
        while (docID1 != null || docID2 != null) 
        {
            if (docID1 <= docID2)
            {
            	newDocList.add(docID1);
            	docID1 = popNextOrNull(docList1);
            }
            else
            {
            	newDocList.add(docID2);
            	docID2 = popNextOrNull(docList2);
            }
            
            if(docID1 == null)
            {
            	while(docID2 != null)
            	{
            		newDocList.add(docID2);
            		docID2 = popNextOrNull(docList2);
            	}
            }
            else if(docID2 == null)
            {
            	while(docID1 != null)
            	{
            		newDocList.add(docID1);
            		docID1 = popNextOrNull(docList1);
            	}
            }
        }
    	termID = p1.getTermId();
        PostingList newPostList = new PostingList(termID,newDocList);
        return newPostList;
    }
    

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
			return;
		}

		/* Get index */
		String className = "";
		try {
			className = args[0];
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		String root = args[1];
		

		/* Get output directory */
		String output = args[2];
		runIndexer(className, root, output);
	}

}
