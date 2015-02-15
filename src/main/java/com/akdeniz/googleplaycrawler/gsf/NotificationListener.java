package com.akdeniz.googleplaycrawler.gsf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private ExecutorService executer;

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
            service.setStopListening();
	    } catch (IOException e) {
		System.out.println("Error occured while downloading " + packageName + " : " + e.getMessage());
	    }

	}

	private void download(String downloadUrl, HttpCookie downloadAuthCookie, String packageName) throws IOException,
		FileNotFoundException {
	    InputStream downloadStream = service.executeDownload(downloadUrl, downloadAuthCookie.getName() + "="
		    + downloadAuthCookie.getValue());

	    FileOutputStream outputStream = new FileOutputStream(packageName + ".apk");

	    byte buffer[] = new byte[8192];
	    for (int k = 0; (k = downloadStream.read(buffer)) != -1;) {
		outputStream.write(buffer, 0, k);
	    }
	    downloadStream.close();
	    outputStream.close();
	}
    }
}
