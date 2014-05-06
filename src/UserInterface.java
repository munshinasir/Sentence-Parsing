import java.util.Scanner;


public class UserInterface {
	
	public static String currentQuery = new String();

	public static void main(String[] args) {
		System.out.println("Welcome to the Olympics QA System.");
		System.out.println("Please ask a question. Type 'q' when finished.");
		System.out.println();
		String input;
		Scanner keyboard = new Scanner(System.in);
		do{		
			input =keyboard.nextLine().trim();
			
			if(!input.equalsIgnoreCase("q")){
				currentQuery = input;
				System.out.println("Query: "+currentQuery);
				
				//Perform any query processing
				Parse p = new Parse(currentQuery);
				p.parseString();
				
				printSQL(p); 
				printAnswer(p); 
				System.out.println();
			}
		}while(!input.equalsIgnoreCase("q"));
		
		keyboard.close();
		System.out.println("Goodbye.");

	}
	
	public static void printSQL(Parse p){
		
		//Print SQL
		System.out.println("<SQL>");
		System.out.println(p.formSQL());
		
	}
	public static void printAnswer(Parse p){
		
		System.out.println("<ANSWER>");
		p.executeSQL();
	}


}
