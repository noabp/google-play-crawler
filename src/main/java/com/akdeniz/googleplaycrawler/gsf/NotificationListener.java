package com.akdeniz.googleplaycrawler.gsf;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.akdeniz.googleplaycrawler.GooglePlayAPI;
import com.akdeniz.googleplaycrawler.GooglePlay.AndroidAppDeliveryData;
import com.akdeniz.googleplaycrawler.GooglePlay.HttpCookie;
import com.akdeniz.googleplaycrawler.GooglePlay.Notification;

/**
 * Handles download notifications.
 *  
 * @author akdeniz
 * 
 */
public class NotificationListener {

    private GooglePlayAPI service;
    private String expectedPackage;
    private String dest;
    private ExecutorService executer;

    public NotificationListener(GooglePlayAPI service, String expectedPackage, String dest) {
	    this(service);
        this.expectedPackage = expectedPackage;
        this.dest = dest;
    }

    public NotificationListener(GooglePlayAPI service) {
        this.service = service;
        this.executer = Executors.newFixedThreadPool(5);
    }

    public void notificationReceived(Notification notification) throws IOException {

	    Thread t = new Thread(new DownloadHandler(notification));
        t.start();
        try {
            t.join(5 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class DownloadHandler implements Runnable {

	private Notification notification;

	public DownloadHandler(Notification notification) {
	    this.notification = notification;
	}

	@Override
	public void run() {

        System.out.println("Running in notification listener");
	    AndroidAppDeliveryData appDeliveryData = notification.getAppDeliveryData();

	    String downloadUrl = appDeliveryData.getDownloadUrl();
	    HttpCookie downloadAuthCookie = appDeliveryData.getDownloadAuthCookie(0);

	    long installationSize = appDeliveryData.getDownloadSize();
	    String packageName = notification.getDocid().getBackendDocid();

	    try {
		System.out.println("Downloading..." + packageName + " : " + installationSize + " bytes");
		download(downloadUrl, downloadAuthCookie, packageName);
		System.out.println("Downloaded! " + packageName + ".apk");
            if(expectedPackage == null || expectedPackage.equals(packageName)) {
                service.setStopListening();
            }
	    } catch (IOException e) {
            e.printStackTrace();
	    }

	}

	private void download(String downloadUrl, HttpCookie downloadAuthCookie, String packageName) throws IOException,
		FileNotFoundException {
	    InputStream downloadStream = service.executeDownload(downloadUrl, downloadAuthCookie.getName() + "="
		    + downloadAuthCookie.getValue());

        System.out.println("Saving app to: " + new File(dest + File.separator + packageName + ".apk").getAbsolutePath());
	    FileOutputStream outputStream = new FileOutputStream(dest + File.separator +packageName + ".apk");

	    byte buffer[] = new byte[8192];
	    for (int k = 0; (k = downloadStream.read(buffer)) != -1;) {
		outputStream.write(buffer, 0, k);
	    }
	    downloadStream.close();
	    outputStream.close();
	}
    }
}
