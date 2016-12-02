package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.joda.time.DateTime;

public class Logging {
	private String filePath;
	private boolean logErrors;

	public boolean isLogErrors() {
		return logErrors;
	}

	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public void writeLog(File outputFile)
	{
		try (FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(DateTime.now().toString() + " (" + outputFile.getName() + "): successfully parsed");
		} catch (IOException ee) {
			ee.printStackTrace();
		}
	}
	
	public void writeErrorLog(File outputFile, Exception e)
	{
		try (FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(DateTime.now().toString() + " (" + outputFile.getName() + "): " + e.getMessage());
		} catch (IOException ee) {
			ee.printStackTrace();
		}
	}
	
	public void writeErrorLog(Exception e)
	{
		try (FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(DateTime.now().toString() + " (GLOBAL ERROR): " + e.getMessage());
		} catch (IOException ee) {
			ee.printStackTrace();
		}
	}
}
