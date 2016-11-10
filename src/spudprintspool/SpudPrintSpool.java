/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spudprintspool;
import javax.print.*;
import java.net.*;
import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import java.io.*;
import java.util.*;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
/**
 *
 * @author David Estes
 */
public class SpudPrintSpool {
    public ArrayList printers;
    public String host; 
    public SpudPrintSpool()
    {
        printers = new ArrayList();
        loadConfig();
        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SpudPrintSpool spooler = new SpudPrintSpool();
        spooler.jobWatcher();
        
    }
    
    public void jobWatcher() {
        while(true)
        {
            try 
            {
                String params = "";
                for(int counter=0;counter < this.printers.size();counter++)
                {
                    ArrayList printer = (ArrayList)this.printers.get(counter);
                    String key = (String)printer.get(1);
                    if (params.equals(""))
                    {
                        params = params + "?keys[]=" + key;
                    }
                    else
                    {
                        params = params + "&keys[]=" + key;
                    }
                }
                URL jobUrl = new URL(this.host + "jobs.json" + params);
                URLConnection yc = jobUrl.openConnection();
                DataInputStream in = new DataInputStream(yc.getInputStream());
                String inputTxt = new String();
                try {
                    byte c = 0;
                    while(true)
                    {
                        c = in.readByte();
                        inputTxt = inputTxt + (char) c;
                    }

                }
                catch(IOException ioException)
                {

                }
                in.close();
                System.out.println(inputTxt);
                JSONArray json = (JSONArray) JSONSerializer.toJSON( inputTxt );        
                for(int counter=0;counter < json.size();counter++)
                {
                    JSONObject printer = json.getJSONObject(counter);
                    
                    try {
                        JSONArray jobs = printer.getJSONArray("spud_print_jobs");
                        if(jobs != null)
                        {
                            System.out.println("We have some work to do!");
                            for(int jobCounter=0;jobCounter<jobs.size();jobCounter++)
                            {
                                JSONObject job = jobs.getJSONObject(jobCounter);
                                for(int printCounter=0;printCounter < this.printers.size();printCounter++)
                                {
                                    ArrayList printerObject = (ArrayList)this.printers.get(printCounter);
                                    if(((String)printerObject.get(1)).equals(printer.getString("access_token")))
                                    {
                                        this.printPostscriptFileToPrinter(job.getInt("id"),job.getString("attachment_file_name"),(String)printerObject.get(0));
                                        
                                    }
                                }
                            }
                        }
                    } catch(net.sf.json.JSONException ex)
                    {
                        System.out.println("No jobs found!");
                    }
                    
                    
                }

                in.close();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    public void loadConfig()
    {
        
                try
        {
            Document document = new SAXBuilder().build(new File("config/printers.xml"));

            Element rootElement = document.getRootElement();
            for(int index=0;index < rootElement.getChildren().size();index++)
            {
                Element subElement = (Element)rootElement.getChildren().get(index);
                if(subElement.getName().toLowerCase().equals("printer")) {
                    ArrayList printer = new ArrayList();
                    printer.add(subElement.getAttributeValue("name"));
                    printer.add(subElement.getAttributeValue("key"));
                    printers.add(printer);
                            
                }
                else if(subElement.getName().toLowerCase().equals("host")) {
                    this.host = subElement.getText();
                }
            }
        }
        catch(java.io.IOException ioe) {
            System.out.println("Printer Configuration File Missing!");
        }
        catch(JDOMException ex) {
         //   ex.printStackTrace();
        }
    }
    
    public void printPostscriptFileToPrinter(Integer jobId,String file,String printer)
    {
        String filepath = this.host + "report_jobs/" + file;
        System.out.println("Printing file: " + filepath + " to printer: " + printer + " -- job: " + jobId);
        try {
            URL jobUrl = new URL(filepath);
            URLConnection yc = jobUrl.openConnection();
            DataInputStream in = new DataInputStream(yc.getInputStream());
            ByteArrayOutputStream f = new ByteArrayOutputStream(); 
            
            int contentSize = yc.getHeaderFieldInt("Content-Length",0);
            long byteCount = 0;
            try{
                byte c = 0;
            
                    while(true)
                    {
                        f.write(in.readByte());
                        byteCount += 1;
                    }

                }
                catch(IOException ioException)
                {
                    System.out.println("IOException: " + ioException.getMessage());
                }
            in.close();
            System.out.println("Received: " + byteCount + " of " + contentSize);
            byte[] buffer = f.toByteArray();
            PrintPdf printPDFFile = new PrintPdf(buffer, "Lot Label: " + file,printer);
	    printPDFFile.print();
            String donePath = this.host + "jobs/" + jobId;
            System.out.println("Done: " + donePath);
            URL jobDoneUrl = new URL(donePath);
            URLConnection jc = jobDoneUrl.openConnection();
            DataInputStream in2 = new DataInputStream(jc.getInputStream());
            try{
                byte c = 0;
            
                    while(true)
                    {
                        c = in.readByte();
                    }

                }
                catch(IOException ioException)
                {

                }
            in.close();
            /*
                try{
            // Find the default service
                    inputTxt = "^XA\n\r^MNM\n\r^FO050,50\n\r^B8N,100,Y,N\n\r^FD1234567\n\r^FS\n\r^PQ3\n\r^XZ";
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            
            //PrintService service = PrintServiceLookup.lookupDefaultPrintService();
            PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, null);
            PrintService service = null;
            for(int printserviceCounter=0;printserviceCounter < services.length;printserviceCounter++)
            {
                PrintService currentService = services[printserviceCounter];
                System.out.println("Service: " + currentService.getName());
                DocFlavor[] flavors = currentService.getSupportedDocFlavors();
                
                if(currentService.getName().equals(printer))
                {
                    service = currentService;
                    for(int x=0;x<flavors.length;x++)
                    {
                        DocFlavor flav = flavors[x];
                        System.out.println(flav.toString());
                    }
                    break;
                }
            }
            if(service == null)
            {
                System.out.println("Printer not ready!");
                return;
            }
            // Create the print job
            DocPrintJob job = service.createPrintJob();
            
            Doc docNew = new SimpleDoc(inputTxt.getBytes(),flavor,null);



            // Print it
            
            job.print(docNew, null);

 

            
            } catch (PrintException e) {
                e.printStackTrace();
            } */
                
        }
          catch (Exception ex) {
              ex.printStackTrace();
          }
    }
}
