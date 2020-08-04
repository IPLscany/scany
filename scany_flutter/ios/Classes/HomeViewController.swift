import WeScan
import Flutter
import Foundation
import PDFKit
import UIKit

@available(iOS 13.0, *)
class HomeViewController: UIViewController, ImageScannerControllerDelegate {    

    var _result:FlutterResult?   

    override func viewDidAppear(_ animated: Bool) {       

        if self.isBeingPresented {
            let scannerVC = ImageScannerController()
            scannerVC.imageScannerDelegate = self
            present(scannerVC, animated: true, completion: nil)
        }  
    }    

    func imageScannerController(_ scanner: ImageScannerController, didFailWithError error: Error) {
        print(error)
        _result!(nil)
        self.dismiss(animated: true)
    }    

    func imageScannerController(_ scanner: ImageScannerController, didFinishScanningWithResults results: ImageScannerResults) {
        // Your ViewController is responsible for dismissing the ImageScannerController
        scanner.dismiss(animated: true)
        

        let imagePath = saveImage(image:results.croppedScan.image)
     _result!(imagePath)
       self.dismiss(animated: true)   
    }
    

    func imageScannerControllerDidCancel(_ scanner: ImageScannerController) {
        // Your ViewController is responsible for dismissing the ImageScannerController
        scanner.dismiss(animated: true)
         _result!(nil)
        self.dismiss(animated: true)
    }
    

    func saveImage(image: UIImage) -> String? {
        // jpegData(compressionQuality:0.0)  // for maximum compression
        guard let data = image.jpegData(compressionQuality: 1) ?? image.pngData() else {
            return nil
        }
        guard let directory = try? FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false) as NSURL else {
            return nil
        }

        let fileName = randomString(length:10);
        let filePath: URL = directory.appendingPathComponent(fileName + ".png")!
        //let filePath2: URL = directory.appendingPathComponent(fileName + ".jpg")!
        let filePath3: URL = directory.appendingPathComponent(fileName + ".pdf")!

        do {
            // for .png
            let fileManager = FileManager.default            

            // Check if file exists
            if fileManager.fileExists(atPath: filePath.path) {
                // Delete file
                try fileManager.removeItem(atPath: filePath.path)
            } else {
                print("File does not exist")
            }      

            /*// for .jpeg
            let fileManager2 = FileManager.default            

            // Check if file exists
            if fileManager2.fileExists(atPath: filePath2.path) {
                // Delete file
                try fileManager2.removeItem(atPath: filePath2.path)
            } else {
                print("File does not exist")
            }*/
            
        // for .pdf below..
        // Create an empty PDF document
        let pdfDocument = PDFDocument()

        // Load or create your UIImage
        //let image = UIImage(....)

        // Create a PDF page instance from your image
      
        let pdfPage = PDFPage(image: image)

        // Insert the PDF page into your document
        pdfDocument.insert(pdfPage!, at: 0)

        // Get the raw data of your PDF document
        let pdfData = pdfDocument.dataRepresentation()

        // The url to save the data to
      
        let url = URL(fileURLWithPath:filePath3.path)

        // Save the data to the url
        try! pdfData!.write(to: url)  

        }
        catch let error as NSError {
            print("An error took place: \(error)")
        }        

        // for .png 
/*
        func WritingData(String path){
            data.write(to: path)
            return path.path
        }
*/

        do {
            try data.write(to: filePath)    // for .png
            //try data.write(to: filePath2)   // for .jpg
            //return filePath.path + "\n" + filePath2.path + "\n" + filePath3.path
            return filePath.path + "\n" + filePath3.path
            
        } catch {
            print(error.localizedDescription)
            return nil
        }
    }
    

    func randomString(length: Int) -> String {
        
        let letters : NSString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let len = UInt32(letters.length)
        
        var randomString = ""
        
        for _ in 0 ..< length {
            let rand = arc4random_uniform(len)
            var nextChar = letters.character(at: Int(rand))
            randomString += NSString(characters: &nextChar, length: 1) as String
        }
        
        return randomString
    }
}
