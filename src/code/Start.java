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

//import edu.emory.mathcs.nlp.common.util.Joiner;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.decode.DecodeConfig;
import edu.emory.mathcs.nlp.decode.NLPDecoder;
//import edu.emory.mathcs.nlp.tokenization.EnglishTokenizer;
//import edu.emory.mathcs.nlp.tokenization.Token;
//import edu.emory.mathcs.nlp.tokenization.Tokenizer;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

public class Start {

	public static void main(String[] args) {
//		args = new String[] { "input", "true" };

		Logging logging = new Logging();
		logging.setLogErrors(false);

		if (args.length > 0) {

			if (args.length > 1) {
				try {
					if (args[1].equalsIgnoreCase("true"))
						logging.setLogErrors(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			try {

				Properties props = new Properties();
				props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse");
				StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

				String configUrl = "config/config.xml";
				DecodeConfig config = null;
				try (InputStream configStream = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(configUrl)) {
					config = new DecodeConfig(configStream);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				NLPDecoder decoder = new NLPDecoder(config);

				try {
					String argsPath = new File(args[0]).getAbsoluteFile().toString();

					Files.walk(Paths.get(argsPath)).sorted().forEach(filePath -> {
						try {
							if (Files.isRegularFile(filePath)) {
								System.out.println(filePath);

								String fileParentDirectory = filePath.getParent().toString();

								String fileName = filePath.getFileName().toString();

								File outputFolder = new File(fileParentDirectory + "_output");

								logging.setFilePath(outputFolder.getAbsolutePath() + "/log.txt");

								File outputFile = new File(fileParentDirectory + "_output/" + fileName);

								outputFolder.mkdirs();

								if (outputFile.exists()) {
									try {
										outputFile.delete();
									}

									catch (Exception e) {
										e.printStackTrace();

										if (logging.isLogErrors()) {
											logging.writeErrorLog(outputFile, e);
										}
									}
								}

								try {
									outputFile.createNewFile();
								} catch (IOException e) {
									e.printStackTrace();

									if (logging.isLogErrors()) {
										logging.writeErrorLog(outputFile, e);
									}
								}

								String docid = fileName.substring(0, fileName.lastIndexOf('.'));

								int sent_id = 1;

								InputStream in = null;

								try {
									in = IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(filePath.toString());

									List<NLPNode[]> sentences = decoder.decodeDocument(in);

									for (NLPNode[] nodes : sentences) {
										try {
											StringBuilder sb = new StringBuilder();

											for (int g = 1; g < nodes.length; g++) {
												sb.append(nodes[g].getWordForm() + " ");
											}
											
											String sentenceStr = sb.toString();
											
											// list of symbols from http://www.fileformat.info/info/unicode/category/Pd/list.htm
											sentenceStr = sentenceStr
													.replaceAll("[\u002D\u058A\u05BE\u1400\u1806\u2010\u2011\u2012\u2013\u2014\u2015\u2E17\u2E1A\u2E3A\u2E3B\u2E40\u301C\u3030\u30A0\uFE31\uFE32\uFE58\uFE63\uFF0D]", "-")
													.replace('\u2010', '-').replace('\u2011', '-')
													.replace('\uFFFD', '-').replace('\u202F', '-')
													.replace(" - ", "-").replace(" / ", "/").trim();

											if (sentenceStr.length() > 0) {
												Annotation sentence = new Annotation(sentenceStr);

												pipeline.annotate(sentence);

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

												switch (rootToken) {
												case "-RSB-":
													rootToken = "]";
													rootLemma = "]";
													break;

												case "-LSB-":
													rootToken = "[";
													rootLemma = "[";
													break;

												case "-RRB-":
													rootToken = ")";
													rootLemma = ")";
													break;

												case "-LRB-":
													rootToken = "(";
													rootLemma = "(";
													break;

												case "-RCB-":
													rootToken = "}";
													rootLemma = "}";
													break;

												case "-LCB-":
													rootToken = "{";
													rootLemma = "{";
													break;
												}

												int i = 0;

												boolean rootOnly = true;

												for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
													rootOnly = false;

													String token = edge.getDependent().word();
													String lemma = edge.getDependent().lemma();
													String pos = edge.getDependent().backingLabel()
															.get(CoreAnnotations.PartOfSpeechAnnotation.class)
															.toString();
													int depid = edge.getDependent().index();
													int govid = edge.getGovernor().index();
													String deptype = edge.getRelation().getShortName();
													String deptype_add = edge.getRelation().toString();
													String ner = "O";

													switch (token) {
													case "-RSB-":
														token = "]";
														lemma = "]";
														break;

													case "-LSB-":
														token = "[";
														lemma = "[";
														break;

													case "-RRB-":
														token = ")";
														lemma = ")";
														break;

													case "-LRB-":
														token = "(";
														lemma = "(";
														break;

													case "-RCB-":
														token = "}";
														lemma = "}";
														break;

													case "-LCB-":
														token = "{";
														lemma = "{";
														break;
													}

													int j = i;

													if (depid - rootDepid == 1) {
														while (!(rootToken.replace(".", "").trim()
																.toLowerCase().equals(nodes[i].getWordForm()
																		.replace(".", "").trim().toLowerCase()))
																&& i < nodes.length - 1) {
															i++;
														}

														if (rootToken.replace(".", "").trim()
																.toLowerCase().equals(nodes[i].getWordForm()
																		.replace(".", "").trim().toLowerCase())
																&& i - j < 10) {
															rootNer = nodes[i].getNamedEntityTag();
															if (nodes[i].getLemma() == "#hlink#")
																rootLemma = nodes[i].getLemma();
														} else
															i = j;

														try (FileWriter fw = new FileWriter(outputFile, true);
																BufferedWriter bw = new BufferedWriter(fw);
																PrintWriter out = new PrintWriter(bw)) {
															out.println(docid + "\t" + sent_id + "\t" + rootToken + "\t"
																	+ rootLemma + "\t" + rootPos + "\t" + rootDepid
																	+ "\t" + 0 + "\t" + rootDeptype + "\t" + null + "\t"
																	+ rootNer);
														} catch (IOException e) {
															e.printStackTrace();

															if (logging.isLogErrors()) {
																logging.writeErrorLog(outputFile, e);
															}
														}

													}

													j = i;

													while (!(token.replace(".", "").trim()
															.toLowerCase().equals(nodes[i].getWordForm()
																	.replace(".", "").trim().toLowerCase()))
															&& i < nodes.length - 1) {
														i++;
													}

													if (token.replace(".", "").trim()
															.toLowerCase().equals(nodes[i].getWordForm()
																	.replace(".", "").trim().toLowerCase())
															&& i - j < 10) {
														ner = nodes[i].getNamedEntityTag();
														if (nodes[i].getLemma() == "#hlink#")
															lemma = nodes[i].getLemma();
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
																+ "\t" + pos + "\t" + depid + "\t" + govid + "\t"
																+ deptype + "\t" + deptype_add + "\t" + ner);
													} catch (IOException e) {
														e.printStackTrace();

														if (logging.isLogErrors()) {
															logging.writeErrorLog(outputFile, e);
														}
													}
												}

												if (rootOnly) {
													int j = i;

													while (!(rootToken.replace(".", "").trim()
															.toLowerCase().equals(nodes[i].getWordForm()
																	.replace(".", "").trim().toLowerCase()))
															&& i < nodes.length - 1) {
														i++;
													}

													if (rootToken.replace(".", "").trim()
															.toLowerCase().equals(nodes[i].getWordForm()
																	.replace(".", "").trim().toLowerCase())
															&& i - j < 10) {
														rootNer = nodes[i].getNamedEntityTag();
														if (nodes[i].getLemma() == "#hlink#")
															rootLemma = nodes[i].getLemma();
													} else
														i = j;

													try (FileWriter fw = new FileWriter(outputFile, true);
															BufferedWriter bw = new BufferedWriter(fw);
															PrintWriter out = new PrintWriter(bw)) {
														out.println(docid + "\t" + sent_id + "\t" + rootToken + "\t"
																+ rootLemma + "\t" + rootPos + "\t" + rootDepid + "\t"
																+ 0 + "\t" + rootDeptype + "\t" + null + "\t"
																+ rootNer);
													} catch (IOException e) {
														e.printStackTrace();

														if (logging.isLogErrors()) {
															logging.writeErrorLog(outputFile, e);
														}
													}
												}

												// System.out.println(docid + ":
												// " +
												// sent_id);

												sent_id++;
											}
										} catch (Exception e) {
											e.printStackTrace();

											if (logging.isLogErrors()) {
												logging.writeErrorLog(outputFile, e);
											}
										}
									}

									in.close();

								} catch (IOException e) {
									e.printStackTrace();

									if (logging.isLogErrors()) {
										logging.writeErrorLog(outputFile, e);
									}
								}

								logging.writeLog(outputFile);

							}
						} catch (Exception x) {
							logging.writeErrorLog(x);
						}
					});

				} catch (IOException e2) {
					e2.printStackTrace();

					if (logging.isLogErrors()) {
						logging.writeErrorLog(e2);
					}
				}
			} catch (Exception exc) {

			}
		}

	}
}
