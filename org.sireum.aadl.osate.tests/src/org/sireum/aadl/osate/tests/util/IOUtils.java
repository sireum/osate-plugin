package org.sireum.aadl.osate.tests.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IOUtils {

	static Properties getPropertiesFile(File f) {
		try {
			FileInputStream fis = new FileInputStream(f);
			Properties prop = new Properties();
			prop.load(fis);
			return prop;
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
		return null;
	}

	public static String readFile(File f) {
		try {
			return new String(Files.readAllBytes(Paths.get(f.toURI())));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	public static void writeFile(File f, String str) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(f.toURI()))) {
			writer.write(str);
			System.out.println("Wrote: " + f);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void zipFile(File f) {
		try {
			String zipFileName = f.getPath().concat(".zip");

			FileOutputStream fos = new FileOutputStream(zipFileName);
			ZipOutputStream zos = new ZipOutputStream(fos);

			ZipEntry ze = new ZipEntry(f.getName());

			// hack to make sure timestamp in the generated zip file is always
			// the same -- ensures git diffs work as expected
			ze.setTime(0);

			zos.putNextEntry(ze);

			byte[] bytes = Files.readAllBytes(Paths.get(f.toURI()));
			zos.write(bytes, 0, bytes.length);
			zos.closeEntry();
			zos.close();

			System.out.println("Wrote: " + zipFileName);
		} catch (FileNotFoundException ex) {
			System.err.format("The file %s does not exist", f);
		} catch (IOException ex) {
			System.err.println("I/O error: " + ex);
		}
	}

	public static List<File> collectFiles(File root, String endsWith, boolean recursive) {
		assert (root.isDirectory());

		List<File> ret = new ArrayList<>();
		for (File f : root.listFiles()) {
			if (f.isFile() && f.getName().endsWith(endsWith)) {
				ret.add(f);
			} else if (f.isDirectory() && recursive) {
				ret.addAll(collectFiles(f, endsWith, recursive));
			}
		}

		return ret;
	}

}
