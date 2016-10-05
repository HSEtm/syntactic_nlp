package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import edu.emory.mathcs.nlp.common.util.Joiner;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.decode.DecodeConfig;
import edu.emory.mathcs.nlp.decode.NLPDecoder;
import edu.emory.mathcs.nlp.tokenization.EnglishTokenizer;
import edu.emory.mathcs.nlp.tokenization.Token;
import edu.emory.mathcs.nlp.tokenization.Tokenizer;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

public class Start {

	public static void main(String[] args) {

//		args = new String[] { "D:/Software for TM/syntactic_nlp/input/wiki.txt" };
		// args = new String[] { "input" };
		// args = new String[] { "D:/Software for
		// TM/syntactic_nlp/input/00000.txt" };
//		args = new String[] { "input/11111.txt" };

		if (args.length > 0) {

			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			Tokenizer tokenizer = new EnglishTokenizer();

			String configUrl = "config/config.xml";
			DecodeConfig config = null;
			try (InputStream configStream = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(configUrl)) {
				config = new DecodeConfig(configStream);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			NLPDecoder decoder = new NLPDecoder(config);

			for (int o = 0; o < args.length; o++) {

				try {
					String argsPath = new File(args[0]).getAbsoluteFile().toString();

					Files.walk(Paths.get(argsPath)).forEach(filePath -> {
						if (Files.isRegularFile(filePath)) {
							System.out.println(filePath);

							String fileParentDirectory = filePath.getParent().getParent().toString();

							String fileName = filePath.getFileName().toString();

							File outputFolder = new File(fileParentDirectory + "/output");

							File outputFile = new File(fileParentDirectory + "/output/" + fileName);

							outputFolder.mkdirs();

							if (outputFile.exists()) {
								try {
									outputFile.delete();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							try {
								outputFile.createNewFile();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							String docid = fileName.substring(0, fileName.lastIndexOf('.'));

							int sent_id = 1;

							InputStream in = null;

							try {
								in = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(filePath.toString());

								for (List<Token> sentences : tokenizer.segmentize(in)) {
									try {
										/*
										 * for (Token token : sentences) {
										 * System.out.print(token + " "); //
										 * TODO delete later }
										 * 
										 * System.out.println();
										 */

										String sentenceStr = Joiner.join(sentences, " ");
										// sentenceStr =
										// sentenceStr.replace(".",
										// "").replace("
										// / ",
										// "/").replace("\\", "/").trim();

										sentenceStr = sentenceStr.replace('\u2010', '-').replace('\u2011', '-').
												replace('\uFFFD', '-').replace('\u202F', '-').replace(".", "").
												replace("\\", "/").replace(" / ", "/").
//												replace('\uF02D', '-').
//												replace('\u9D00', '-').replace('\uFFFD', '-').replace('\uFFFC', '-').
												trim();

										if (sentenceStr.length() > 0) {
											
											NLPNode[] nodes = decoder.decode(sentenceStr);

											Annotation sentence = new Annotation(sentenceStr);

											// run all Annotators on this text
											pipeline.annotate(sentence);

											// this is the Stanford dependency
											// graph
											// of
											// the
											// current
											// sentence
											SemanticGraph dependencies = sentence
													.get(CoreAnnotations.SentencesAnnotation.class).get(0)
													.get(CollapsedCCProcessedDependenciesAnnotation.class);

											String rootToken = dependencies.getFirstRoot().word();
											String rootLemma = dependencies.getFirstRoot().lemma();
											String rootPos = dependencies.getFirstRoot().backingLabel()
													.get(CoreAnnotations.PartOfSpeechAnnotation.class).toString();
											int rootDepid = dependencies.getFirstRoot().index();
											String rootDeptype = "root";
											String rootNer = "O";

											int i = 0;

											boolean rootOnly = true;

											for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
												rootOnly = false;

												String token = edge.getDependent().word();
												String lemma = edge.getDependent().lemma();
												String pos = edge.getDependent().backingLabel()
														.get(CoreAnnotations.PartOfSpeechAnnotation.class).toString();
												int depid = edge.getDependent().index();
												int govid = edge.getGovernor().index();
												String deptype = edge.getRelation().getShortName();
												String deptype_add = edge.getRelation().toString();
												String ner = "O";

												int j = i;

												if (depid - rootDepid == 1) {
													while (!(rootToken.replace(".", "").trim().toLowerCase() .equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase()))
															&& i < nodes.length - 1) {
														i++;
													}

													if (rootToken.replace(".", "").trim().toLowerCase() .equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase())
															&& i - j < 10) {
														rootNer = nodes[i].getNamedEntityTag();
													} else
														i = j;
													

													try (FileWriter fw = new FileWriter(outputFile, true);
															BufferedWriter bw = new BufferedWriter(fw);
															PrintWriter out = new PrintWriter(bw)) {
														out.println(docid + "\t" + sent_id + "\t" + rootToken + "\t"
																+ rootLemma + "\t" + rootPos + "\t" + rootDepid + "\t"
																+ null + "\t" + rootDeptype + "\t" + null + "\t"
																+ rootNer);
													} catch (IOException e) {
														e.printStackTrace();
													}

												}
												
												j = i;

												while (!(token.replace(".", "").trim().toLowerCase().equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase()))
														&& i < nodes.length - 1) {

//													System.out.println(token.replace(".", "").trim().toLowerCase() + " - " + nodes[i].getWordForm().replace(".", "").trim().toLowerCase());
													i++;
												}

												if (token.replace(".", "").trim().toLowerCase() .equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase())
														&& i - j < 10) {
													ner = nodes[i].getNamedEntityTag();
												} else
													i = j;

												int symbol = deptype.indexOf(":");

												if (symbol >= 0 && deptype == deptype_add) {
													deptype = deptype.substring(0, symbol);
												}

												symbol = deptype_add.indexOf(":");

												if (symbol >= 0) {
													deptype_add = deptype_add.substring(symbol + 1);
												} else
													deptype_add = null;

												try (FileWriter fw = new FileWriter(outputFile, true);
														BufferedWriter bw = new BufferedWriter(fw);
														PrintWriter out = new PrintWriter(bw)) {
													out.println(docid + "\t" + sent_id + "\t" + token + "\t" + lemma
															+ "\t" + pos + "\t" + depid + "\t" + govid + "\t" + deptype
															+ "\t" + deptype_add + "\t" + ner);
												} catch (IOException e) {
													e.printStackTrace();
												}
											}

											if (rootOnly) {
												int j = i;

												while (!(rootToken.replace(".", "").trim().toLowerCase() .equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase()))
														&& i < nodes.length - 1) {
													i++;
												}

												if (rootToken.replace(".", "").trim().toLowerCase() .equals(nodes[i].getWordForm().replace(".", "").trim().toLowerCase())
														&& i - j < 10) {
													rootNer = nodes[i].getNamedEntityTag();
												} else
													i = j;

												try (FileWriter fw = new FileWriter(outputFile, true);
														BufferedWriter bw = new BufferedWriter(fw);
														PrintWriter out = new PrintWriter(bw)) {
													out.println(docid + "\t" + sent_id + "\t" + rootToken + "\t"
															+ rootLemma + "\t" + rootPos + "\t" + rootDepid + "\t"
															+ null + "\t" + rootDeptype + "\t" + null + "\t" + rootNer);
												} catch (IOException e) {
													e.printStackTrace();
												}
											}

											// System.out.println(docid + ": " +
											// sent_id);

											sent_id++;
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								in.close();

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					});

				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		}

	}
}
