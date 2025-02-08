package ict.pag.webframework.preInstrumental;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;

public class JarHandler {
	public void replaceMultiJarFiles(String jarPathAndName, ArrayList<byte[]> fileByteCodes, ArrayList<String> fileNames, String suffix, String outPath)
			throws IOException {
		File jarFile = new File(jarPathAndName);
		String jarname = jarFile.getName();
		String updatedJarName = outPath + File.separator + jarname.substring(0, jarname.lastIndexOf('.')) + suffix + ".jar";
		File tempJarFile = new File(updatedJarName);

		JarFile jar = new JarFile(jarFile);
		boolean jarWasUpdated = false;

		try {
			JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile));

			// Allocate a buffer for reading entry data.
			byte[] buffer = new byte[1024];
			int bytesRead;

			try {
				// Open the given file.

				try {
					for (int i = 0; i < fileNames.size(); i++) {
						String fileName = fileNames.get(i);
						byte[] fileByteCode = fileByteCodes.get(i);
						// Create a jar entry and add it to the temp jar.
						JarEntry entry = new JarEntry(fileName);
						if (fileName.endsWith(".jar")) {
							entry.setMethod(0); /* set store method for uncompressed entries, stored */
							entry.setSize(fileByteCode.length);
							CRC32 crc = new CRC32();
							crc.update(fileByteCode);
							entry.setCrc(crc.getValue());
						}
						entry.setCompressedSize(-1);
						tempJar.putNextEntry(entry);
						tempJar.write(fileByteCode);
						System.out.println("[jar write done]" + fileName);
					}
				} catch (Exception ex) {
					System.out.println(ex);
					// Add a stub entry here, so that the jar will close without an
					// exception.
					tempJar.putNextEntry(new JarEntry("stub"));
				}

				// Loop through the jar entries and add them to the temp jar,
				// skipping the entry that was added to the temp jar already.
				InputStream entryStream = null;
				for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
					// Get the next entry.
					JarEntry entry = (JarEntry) entries.nextElement();

					// If the entry has not been added already, so add it.
//					if (!entry.getName().equals(fileName)) {
					if (!fileNames.contains(entry.getName())) {
						// Get an input stream for the entry.
						entryStream = jar.getInputStream(entry);
						entry.setCompressedSize(-1);
						tempJar.putNextEntry(entry);

						while ((bytesRead = entryStream.read(buffer)) != -1) {
							tempJar.write(buffer, 0, bytesRead);
						}
					} else
						System.out.println("[already write]" + entry.getName());
				}
				if (entryStream != null)
					entryStream.close();

				jarWasUpdated = true;
			} catch (Exception ex) {
				System.out.println(ex);
				// IMportant so the jar will close without an
				// exception.
				tempJar.putNextEntry(new JarEntry("stub"));
			} finally {
				tempJar.close();
			}
		} finally {

			jar.close();

			if (!jarWasUpdated) {
				tempJarFile.delete();
			}
		}
	}

	public void replaceSingleJarFile(String jarPathAndName, byte[] fileByteCode, String fileName, String suffix, String outPath) throws IOException {
//		String updatedJarName = jarPathAndName.substring(0, jarPathAndName.lastIndexOf('.')) + suffix + ".jar";
//		File tempJarFile = new File(updatedJarName);

		File jarFile = new File(jarPathAndName);
		String jarname = jarFile.getName();
		String updatedJarName = outPath + File.separator + jarname.substring(0, jarname.lastIndexOf('.')) + suffix + ".jar";
		File tempJarFile = new File(updatedJarName);

		JarFile jar = new JarFile(jarFile);
		boolean jarWasUpdated = false;

		try {
			JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile));

			// Allocate a buffer for reading entry data.

			byte[] buffer = new byte[1024];
			int bytesRead;

			try {
				// Open the given file.

				try {
					// Create a jar entry and add it to the temp jar.
					JarEntry entry = new JarEntry(fileName);
					if (fileName.endsWith(".jar")) {
						entry.setMethod(0); /* set store method for uncompressed entries, stored */
						entry.setSize(fileByteCode.length);
						CRC32 crc = new CRC32();
						crc.update(fileByteCode);
						entry.setCrc(crc.getValue());
					}
					entry.setCompressedSize(-1);
					tempJar.putNextEntry(entry);
					tempJar.write(fileByteCode);
					System.out.println("[jar write done]" + fileName);
				} catch (Exception ex) {
					System.out.println(ex);
					// Add a stub entry here, so that the jar will close without an
					// exception.
					tempJar.putNextEntry(new JarEntry("stub"));
				}

				// Loop through the jar entries and add them to the temp jar,
				// skipping the entry that was added to the temp jar already.
				InputStream entryStream = null;
				for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
					// Get the next entry.
					JarEntry entry = (JarEntry) entries.nextElement();

					// If the entry has not been added already, so add it.
					if (!entry.getName().equals(fileName)) {
						// Get an input stream for the entry.
						entryStream = jar.getInputStream(entry);
						entry.setCompressedSize(-1);
						tempJar.putNextEntry(entry);

						while ((bytesRead = entryStream.read(buffer)) != -1) {
							tempJar.write(buffer, 0, bytesRead);
						}
					} else
						System.out.println("[already write]" + fileName);
				}
				if (entryStream != null)
					entryStream.close();

				jarWasUpdated = true;
			} catch (Exception ex) {
				System.out.println(ex);

				// IMportant so the jar will close without an
				// exception.

				tempJar.putNextEntry(new JarEntry("stub"));
			} finally {
				tempJar.close();
			}
		} finally {

			jar.close();

			if (!jarWasUpdated) {
				tempJarFile.delete();
			}
		}
// 		remove old jar
//		if (jarWasUpdated) {
//			// default policy 1: retain both
//
//			// policy 2: remove original
//			if (jarFile.delete()) {
//				tempJarFile.renameTo(jarFile);
//				System.out.println(jarPathAndName + " updated.");
//			} else
//				System.out.println("Could Not Delete JAR File");
//		}
	}
}
