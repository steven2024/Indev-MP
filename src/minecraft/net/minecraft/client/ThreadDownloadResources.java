package net.minecraft.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public final class ThreadDownloadResources extends Thread {
	private File resourcesFolder;
	private Minecraft mc;
	private boolean closing = false;

	public ThreadDownloadResources(File file, Minecraft minecraft) {
		this.mc = minecraft;
		this.setName("Resource download thread");
		this.setDaemon(true);
		this.resourcesFolder = new File(file, "resources/");
		if(!this.resourcesFolder.exists() && !this.resourcesFolder.mkdirs()) {
			throw new RuntimeException("The working directory could not be created: " + this.resourcesFolder);
		}
	}

	public final void run() {
		try {
			ArrayList<String> arrayList2 = new ArrayList<String>();
			URL uRL3 = new URL("http://www.minecraft.net/resources/");
			BufferedReader bufferedReader4 = new BufferedReader(new InputStreamReader(uRL3.openStream()));
			String string5;
			while ((string5 = bufferedReader4.readLine()) != null) {
				arrayList2.add(string5);
			}
			bufferedReader4.close();
			for (int i = 0; i < arrayList2.size(); ++i) {
				URL url2 = uRL3;
				String string7 = arrayList2.get(i);
				URL uRL6 = url2;
				Label_0334: {
					try {
						String[] split;
						String string8 = (split = string7.split(","))[0];
						int integer9 = Integer.parseInt(split[1]);
						Long.parseLong(split[2]);
						File file7;
						if (!(file7 = new File(this.resourcesFolder, string8)).exists() || file7.length() != integer9) {
							file7.getParentFile().mkdirs();
							this.downloadResource(new URL(uRL6, string8.replaceAll(" ", "%20")), file7);
							if (this.closing) {
								break Label_0334;
							}
						}
						Minecraft b = this.mc;
						String s3 = string8;
						String string6 = s3;
						Minecraft minecraft5 = b;
						int integer8 = string6.indexOf("/");
						String string9 = string6.substring(0, integer8);
						string6 = string6.substring(integer8 + 1);
						if (string9.equalsIgnoreCase("sound")) {
							minecraft5.sndManager.addSound(string6, file7);
						}
						else if (string9.equalsIgnoreCase("newsound")) {
							minecraft5.sndManager.addSound(string6, file7);
						}
						else if (string9.equalsIgnoreCase("music")) {
							minecraft5.sndManager.addMusic(string6, file7);
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				if (this.closing) {
					return;
				}
			}
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
		}
	}

	private void downloadResource(URL url, File file) throws IOException {
		byte[] b3 = new byte[4096];
		DataInputStream url1 = new DataInputStream(url.openStream());
		DataOutputStream file1 = new DataOutputStream(new FileOutputStream(file));

		do {
			int i4;
			if((i4 = url1.read(b3)) < 0) {
				url1.close();
				file1.close();
				return;
			}

			file1.write(b3, 0, i4);
		} while(!this.closing);

	}

	public final void closeMinecraft() {
		this.closing = true;
	}
}
