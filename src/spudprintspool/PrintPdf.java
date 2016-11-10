/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spudprintspool;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import javax.print.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

/**
 * Converts the PDF content into printable format
 */
public class PrintPdf {

	private PrinterJob pjob = null;
        public String printerName = null;
	public static void main(String[] args) throws IOException, PrinterException {
		if (args.length != 1) {
			System.err.println("The first parameter must have the location of the PDF file to be printed");
		}
		System.out.println("Printing: " + args[0]);
		// Create a PDFFile from a File reference
		FileInputStream fis = new FileInputStream(args[0]);
		PrintPdf printPDFFile = new PrintPdf(fis, "Test Print PDF",null);
		printPDFFile.print();
	}

	/**
	 * Constructs the print job based on the input stream
	 * 
	 * @param inputStream
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	public PrintPdf(InputStream inputStream, String jobName,String printer) throws IOException, PrinterException {
                this.printerName = printer;
		byte[] pdfContent = new byte[inputStream.available()];
		inputStream.read(pdfContent, 0, inputStream.available());
		initialize(pdfContent, jobName);
	}

	/**
	 * Constructs the print job based on the byte array content
	 * 
	 * @param content
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	public PrintPdf(byte[] content, String jobName,String printer) throws IOException, PrinterException {
                this.printerName = printer;
		initialize(content, jobName);
	}

	/**
	 * Initializes the job
	 * 
	 * @param pdfContent
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	private void initialize(byte[] pdfContent, String jobName) throws IOException, PrinterException {
		ByteBuffer bb = ByteBuffer.wrap(pdfContent);
		// Create PDF Print Page
		PDFFile pdfFile = new PDFFile(bb);
		PDFPrintPage pages = new PDFPrintPage(pdfFile);

		// Create Print Job
		pjob = PrinterJob.getPrinterJob();
                
                if(this.printerName != null)
                {
                    PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                    PrintService service = null;
                    for(int printserviceCounter=0;printserviceCounter < services.length;printserviceCounter++)
                    {
                        PrintService currentService = services[printserviceCounter];
                        System.out.println("Service: " + currentService.getName());
                        DocFlavor[] flavors = currentService.getSupportedDocFlavors();

                        if(currentService.getName().equals(this.printerName))
                        {
                            pjob.setPrintService(currentService);
                          
                            break;
                        }
                    }
                }
                
                
		PageFormat pf = PrinterJob.getPrinterJob().defaultPage();
                //pf.
		pjob.setJobName(jobName);
		Book book = new Book();
		book.append(pages, pf, pdfFile.getNumPages());
		pjob.setPageable(book);

		// to remove margins
		Paper paper = new Paper();
                paper.setSize(216, 5*72);
		paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
		pf.setPaper(paper);
	}

	public void print() throws PrinterException {
		// Send print job to default printer
		pjob.print();
	}
}

/**
 * Class that actually converts the PDF file into Printable format
 */
class PDFPrintPage implements Printable {

	private PDFFile file;

	PDFPrintPage(PDFFile file) {
		this.file = file;
	}

	public int print(Graphics g, PageFormat format, int index) throws PrinterException {
		int pagenum = index + 1;
		if ((pagenum >= 1) && (pagenum <= file.getNumPages())) {
			Graphics2D g2 = (Graphics2D) g;
			PDFPage page = file.getPage(pagenum);

			// fit the PDFPage into the printing area
			Rectangle imageArea = new Rectangle((int) format.getImageableX(), (int) format.getImageableY(),
					(int) format.getImageableWidth(), (int) format.getImageableHeight());
			g2.translate(0, 0);
			PDFRenderer pgs = new PDFRenderer(page, g2, imageArea, null, null);
			try {
				page.waitForFinish();
				pgs.run();
			} catch (InterruptedException ie) {
				// nothing to do
			}
			return PAGE_EXISTS;
		} else {
			return NO_SUCH_PAGE;
		}
	}
}