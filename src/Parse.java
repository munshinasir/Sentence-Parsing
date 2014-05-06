import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/*
 * All the parsing related functions appear here
 */
public class Parse {

	String query;
	LexicalizedParser lp;
	TreebankLanguagePack tlp;
	GrammaticalStructureFactory gsf;
	List<String> result;
	List<String> select;
	List<String> from;
	List<String> where;
	List<String> innerJoin;
	HashMap<String, TypedDependency> relations;
	private final HashMap<String, String> nationalities;
	HashMap<String, String> medals;
	List<String> events;

	public Parse(String query) {

		lp = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
		result = new ArrayList<String>();
		select = new ArrayList<String>();
		from = new ArrayList<String>();
		where = new ArrayList<String>();
		innerJoin = new ArrayList<String>();
		this.query = query;
		relations = new HashMap<String, TypedDependency>();

		/*
		 * Initializing some of the mappings
		 */
		nationalities = new HashMap<String, String>();

		nationalities.put("austrian", "austria");
		nationalities.put("canadian", "canada");
		nationalities.put("chinese", "china");
		nationalities.put("czech", "czech republic");
		nationalities.put("french", "france");
		nationalities.put("german", "germany");
		nationalities.put("dutch", "netherlands");
		nationalities.put("norwegian", "norway");
		nationalities.put("polish", "poland");
		nationalities.put("russian", "russia");
		nationalities.put("slovakian", "slovakia");
		nationalities.put("slovenian", "slovenia");
		nationalities.put("ukrainian", "ukraine");
		nationalities.put("american", "usa");

		medals = new HashMap<String, String>();
		medals.put("gold", "gold");
		medals.put("silver", "silver");
		medals.put("bronze", "bronze");
		medals.put("first", "gold");
		medals.put("second", "silver");
		medals.put("third", "bronze");

		events = new ArrayList<String>();
		events.add("biathlon");
		events.add("skijumping");
		events.add("speedskating");
		events.add("shorttrack");
		events.add("icedancing");
		events.add("giantslalom");
		events.add("crosscountry");
		events.add("slalom");
		events.add("super-combined");
	}

	/*
	 * The first assignment code Only for referral purposes
	 */
	public void parseCorpus() {

		// Loading the parser
		DocumentPreprocessor doc = new DocumentPreprocessor(
				"data/sentences.txt");

		PrintWriter gsWriter = null;

		try {
			PrintWriter treeWriter = new PrintWriter(new File(
					"output/parseTrees.txt"));
			gsWriter = new PrintWriter(new File("output/grammarStructures.txt"));

			for (List<HasWord> sentence : doc) {
				Tree parse = lp.apply(sentence);
				parse.pennPrint(treeWriter);
				parse.pennPrint();
				System.out.println();

				GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
				Collection tdl = gs.typedDependenciesCCprocessed();
				System.out.println(tdl);
				gsWriter.println(tdl);
				System.out.println();
			}

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} finally {

			gsWriter.close();

		}

	}

	/*
	 * Generating the parse tree for the string
	 */
	public void parseString() {

		// getting the parse tree
		Tree parseTree = lp.parse(query);
		// parseTree.pennPrint();

		// getting the grammar structure
		// GrammaticalStructure gs = gsf.newGrammaticalStructure(parseTree);
		EnglishGrammaticalStructure gs = new EnglishGrammaticalStructure(
				parseTree);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		System.out.println(tdl);

		// add them to the HashMap
		for (TypedDependency td : tdl) {

			relations.put(td.reln().toString(), td);

		}

		// get the root
		TypedDependency rootDep = getRoot(tdl);

		if (rootDep != null) {

			// if rootDep.dep() is 'win'/'won'/'arrived' just plain and simple
			// attach the from results R s
			// string

			if (rootDep.dep().nodeString().toLowerCase().equals("win")
					|| rootDep.dep().nodeString().toLowerCase().equals("won")
					|| rootDep.dep().nodeString().toLowerCase()
							.equals("arrived")
					|| rootDep.dep().nodeString().toLowerCase()
							.equals("arrive")) {

				from.add("results R");
				innerJoin
						.add("INNER JOIN competitions C on R.comp_id = C.comp_id ");

			}

			for (String rel : relations.keySet()) {

				if (!rel.equals("root")) {

					TypedDependency td = relations.get(rel);

					switch (rel) {

					case "nsubj":

						if (td.gov().equals(rootDep.dep())) {
							// processing the nsubj dep
							TreeGraphNode nsubDep = td.dep();

							if (nsubDep.parent().parent().children().length == 1) {

								if (nsubDep.parent().parent().nodeString()
										.equals("WHNP")) {

									select.add("R.winner");

								} else if (nsubDep.parent().parent()
										.nodeString().equals("NP")) {
									// here the dep is a part of the where
									// clause
									where.add("R.winner LIKE \"%"
											+ nsubDep.nodeString() + "\"");
								}

							}

							else {
								if (nsubDep.parent().parent().children().length == 3) {

									if (nsubDep.nodeString().toLowerCase()
											.equals("man"))
										where.add("A.gender = \"M\"");
									else if (nsubDep.nodeString().toLowerCase()
											.equals("woman"))
										where.add("A.gender = \"F\"");
								}
							}
						}
						break;

					case "nn":

						// nn can be for nsubj or for dobj
						// checking for nsubj
						if (nationalities.containsKey(td.dep().nodeString()
								.toLowerCase())) {

							where.add("A.nationality = \""
									+ nationalities.get(td.dep().nodeString()
											.toLowerCase()) + "\"");

						}
						break;

					case "amod":

						// amod can be for nsubj or for dobj
						// checking for nsubj
						if (events
								.contains(td.gov().nodeString().toLowerCase())) {

							String query = td.dep().nodeString().toLowerCase();

							// processing for 1000m
							if (query.endsWith("m")) {

								query = query.substring(0, query.length() - 1);
								System.out.println(query);

							}

							where.add("C.type = \"" + query + "\"");

						} else if (nationalities.containsKey(td.dep()
								.nodeString().toLowerCase())) {

							where.add("A.nationality = \""
									+ nationalities.get(td.dep().nodeString()
											.toLowerCase()) + "\"");

						}
						break;
					case "advmod":
						if (td.gov().equals(rootDep.dep())) {

							TreeGraphNode dobjDep = td.dep();
							if (medals.get(dobjDep.nodeString()) != null) {

								where.add("R.medal = \""
										+ medals.get(dobjDep.nodeString())
										+ "\"");

							}
							if (events.contains(dobjDep.nodeString())) {

								where.add("C.name = \"" + td.dep().nodeString()
										+ "\"");

							}
						}
						break;
					case "prep_in":
					case "prepc_in":

						if (events.contains(td.dep().nodeString())) {

							where.add("C.name = \"" + td.dep().nodeString()
									+ "\"");

						}

						break;
					case "det":

						if (td.dep().parent().nodeString().equals("WDT")) {

							select.add("A.name");
							innerJoin
									.add("INNER JOIN athletes A on R.winner = A.name");
						}

						break;
					case "dobj":

						// processing the obj
						if (td.gov().equals(rootDep.dep())) {

							TreeGraphNode dobjDep = td.dep();
							if (medals.get(dobjDep.nodeString()) != null) {

								where.add("R.medal = \""
										+ medals.get(dobjDep.nodeString())
										+ "\"");

							} else if (events.contains(dobjDep.nodeString())) {

								where.add("C.name = \"" + dobjDep.nodeString()
										+ "\"");

							}

						}
						break;
					case "aux":
						// if aux(main verb , did)

						if (td.gov().equals(rootDep.dep())
								&& td.dep().nodeString().toLowerCase()
										.equals("did")) {
							select.add("Count(*)");
							innerJoin
									.add("INNER JOIN athletes A on R.winner = A.name");
						}
						break;

					case "acomp":

						if (td.gov().equals(rootDep.dep())) {

							TreeGraphNode dobjDep = td.dep();
							if (medals.get(dobjDep.nodeString()) != null) {

								where.add("R.medal = \""
										+ medals.get(dobjDep.nodeString())
										+ "\"");

							}

						}

						break;

					case "xcomp":

						if (td.gov().equals(rootDep.dep())) {

							if (events.contains(td.dep().nodeString())) {

								where.add("C.name = \"" + td.dep().nodeString()
										+ "\"");

							}

						}

						break;

					}

				}

			}

			// System.out.println(formSQL());
			// executeSQL();

		} else
			System.out.println("No root provided so cant process the string");

	}

	public TypedDependency getRoot(List<TypedDependency> tdl) {

		TypedDependency root = null;

		for (TypedDependency td : tdl) {

			if (td.gov().nodeString().toLowerCase().equals("root")) {

				root = td;
				return root;

			}

		}

		return root;

	}

	public String formSQL() {

		StringBuilder sql = new StringBuilder();

		// Check for the sport type
		if (where.size() == 1) {

			// checking if the where is only about competition
			if (!select.contains("C.type")) {

				if (where.get(0).contains("C.name =")) {

					select.add("C.type");
					select.add("R.medal");

				}
			}
		}

		// SELECT x,y
		int i;
		if (select.size() > 0 && from.size() > 0) {

			sql.append("SELECT ");

			for (i = 0; i < select.size() - 1; i++) {

				sql.append(select.get(i) + ", ");
				// adding this to an array for further A.name ==> name
				// only if the results are not already populated
				if (!(result.size() == select.size())) {
					if (select.get(i).contains(".")) {
						String[] arr = select.get(i).split("\\.");
						result.add(arr[1]);
					} else
						result.add(select.get(i));
				}
			}
			sql.append(select.get(i) + " ");
			// adding this to an array for further
			if (!(result.size() == select.size())) {
				if (select.get(i).contains(".")) {
					String[] arr = select.get(i).split("\\.");
					result.add(arr[1]);
				} else
					result.add(select.get(i));
			}
			// FROM w, z
			sql.append("FROM ");
			for (i = 0; i < from.size() - 1; i++) {

				sql.append(from.get(i) + ", ");

			}
			sql.append(from.get(i) + " ");

		} else
			return "Sorry we can't form the sql : Please try a different query";

		// INNER JOIN x on ....
		// each string in the list is the complete syntax
		if (innerJoin.size() > 0) {
			for (String inner : innerJoin)
				sql.append(inner + " ");
		}

		// WHERE
		if (where.size() > 0) {
			sql.append("WHERE ");
			for (i = 0; i < where.size() - 1; i++) {

				sql.append(where.get(i) + " AND ");

			}
			sql.append(where.get(i) + " ");
		} else {
			return "No filter condition provided : Malformed query. Are you missing the end ? ";
		}

		sql.append(";");

		return sql.toString();

	}

	public void executeSQL() {

		String query = this.formSQL();
		if (query.contains("Sorry")) {
			return;
		} else if (query.contains("No filter")) {
			return;
		} else {
			this.execute(query);
		}

	}

	public void execute(String query) {

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:Olympics.sqlite");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			StringBuilder sb = null;
			while (rs.next()) {

				sb = new StringBuilder();

				for (String sel : result) {

					// The select is of the form A.name
					if(sel.equals("Count(*)")){
						
						if(rs.getString(sel).equals("0")){
							
							sb.append("No" + " ");
							
						}
						else{
							
							sb.append("Yes" + " ");
							
						}
							
						
					}else{
						sb.append(rs.getString(sel) + " ");	
					}
					
				}

				System.out.println(sb.toString() + "\n");

			}

		} catch (Exception e) {
			System.out.println("Error processing the query - ");
			System.err.println(e.getClass().getName() + ": " + e.getMessage());

		}

	}

	/*
	 * public static void main(String[] args) {
	 * 
	 * Parse p = new Parse("Did Smeekens win silver in the 500m speedskating?");
	 * p.parseString(); }
	 */

}
