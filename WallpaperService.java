package services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.qihoo.qstore.Qstore;
import models.Category;
import models.Wallpaper;
import models.enums.Status;

import models.stores.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import tools.*;
import tools.Store;

public class WallpaperService {
	
	private static final String SINGLE_PKG = Constants.PKG_ILAUNCHER;
	private static final String DOUBLE_PKG = Constants.PKG_LAUNCHER;
	private static final String ORIGIN_PREFIX = "/wallpapers/images/origin/";
	private static final String CROP_PREFIX = "/wallpapers/images/crop/";
	private static final String ZIPS_PREFIX = "/wallpapers/zips/";
	private static final int COVER_WIDTH = 120;
	private static final int COVER_HEIGHT = 100;
	public final static EasyMap<Integer, Integer> WALLPAPER_SIZE = new EasyMap<Integer, Integer>(120, 100)
    		.easyPut(160, 133).easyPut(240, 200).easyPut(320, 266).easyPut(360, 300)
    		.easyPut(480, 400).easyPut(460, 383).easyPut(144, 120);

	/**
	 * 壁纸上传信息验证
	 * @return 错误信息
	 * @throws IOException 
	 */
	public static List<Map<String, String>> verifyInfo(File file, String name, String author, String category, 
			int minw, int maxw, int minh, int maxh) throws IOException {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		if (file == null || file.length() < 1) {
			ErrorMsgUtils.putErrorMsg(results, "请上传文件!", "file");
		} else if (!Store.getExtension(file.getName()).equals("jpg") || Store.getExtension(file.getName()).equals("png")) {
			ErrorMsgUtils.putErrorMsg(results, "请上传jpg或png图片!", "file");
		} else {
			BufferedImage image = ImageIO.read(file);
			if (image.getWidth() != 960 || image.getHeight() != 800) {
				ErrorMsgUtils.putErrorMsg(results, "请上传960x800规格的图片!", "file");
			}
		}
		if (StringUtils.isBlank(name)) {
			ErrorMsgUtils.putErrorMsg(results, "请填写壁纸名称!", "name");
		}
		if (StringUtils.isBlank(author)) {
			ErrorMsgUtils.putErrorMsg(results, "请填写壁纸作者!", "author");
		}
		if (maxw < minw && maxh < minh) {
			ErrorMsgUtils.putErrorMsg(results, "适配终端宽度或高度值错误!", "terminal");
		}
		return results;
	}
	
	/**
	 * 验证批量上传信息
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static List<Map<String, String>> verifyInfo(File file) throws IOException {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		if (file == null || file.length() < 1) {
			ErrorMsgUtils.putErrorMsg(results, "请上传文件!", "file");
		} else if (!Store.getExtension(file.getName()).equals("zip")) {
			ErrorMsgUtils.putErrorMsg(results, "请上传zip格式的图片集!", "file");
		}
//		else {
//			FileInputStream fis = new FileInputStream(file);
//			ZipInputStream zis = new ZipInputStream(fis);
//			ZipEntry entry;
//			BufferedImage image;
//			File targetDir = ZipUtils.extract(file);
//			while ((entry = zis.getNextEntry()) != null) {
//				String ext = FilenameUtils.getExtension(entry.getName()).toLowerCase();
//				if (!ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
//					ErrorMsgUtils.putErrorMsg(results, entry.getName() + "文件格式错误!", "file");
//				}
//				image = ImageIO.read(new File(targetDir + "//" + entry.getName()));
//				if (image.getWidth() != 960 || image.getHeight() != 800) {
//					ErrorMsgUtils.putErrorMsg(results, entry.getName() + "尺寸错误！", "file");
//				}
//			}
//		}
		return results;
	}
	
	/**
	 * 检验zip包里图片合法性
	 * @param rootDir 壁纸文件夹
	 * @return 错误信息
	 * @throws IOException
	 */
	public static List<Map<String, String>> verifyZip(File rootDir) throws IOException {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		BufferedImage bi;
		File[] images = rootDir.listFiles();
		for (File image : images) {
			String ext = FilenameUtils.getExtension(image.getName()).toLowerCase();
			if (!ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
				ErrorMsgUtils.putErrorMsg(results, image.getName() + "文件格式错误!", "file");
			}
			bi = ImageIO.read(new File(rootDir + "//" + image.getName()));
			if (bi.getWidth() != 960 || bi.getHeight() != 800) {
				ErrorMsgUtils.putErrorMsg(results, image.getName() + "尺寸错误！", "file");
			}
		}
		return results;
	}
	
	/**
	 * 根据上传信息生成Wallpaper对象，并生成和保存Image、Zip静态文件
	 * @param file 				上传文件
	 * @param name				名称
	 * @param author			作者
	 * @param categories			分类
	 * @param pkgForTerminals	适配目标包名
	 * @param minw				适配终端的最小宽度
	 * @param maxw				适配终端的最大宽度
	 * @param minh				适配终端的最小高度
	 * @param maxh				适配终端的最大高度
	 * @return	Wallpaper对象
	 * @throws IOException
	 */
	public static Wallpaper createWallpaper(File file, String name, String author, Set<Category> categories, Set<String> pkgForTerminals,
			int minw, int maxw, int minh, int maxh) throws IOException {
		Date date = new Date();
		Wallpaper wallpaper = new Wallpaper();
		wallpaper.code = UUID.randomUUID().toString();
		wallpaper.name = name;
		wallpaper.author = author;
		wallpaper.source = "baibian";
		wallpaper.size = file.length();
		wallpaper.downloadCount = 0;
		wallpaper.browseCount = 0;
		wallpaper.createAt = date;
		wallpaper.categories = categories;
		wallpaper.pkgForTerminals = pkgForTerminals;
		wallpaper.status = Status.VERIFY;
		if (maxw > 0 && maxh > 0) {
			wallpaper.minHeightForTerminal = minh;
			wallpaper.minWidthForTerminal = minw;
			wallpaper.maxHeightForTerminal = maxh;
			wallpaper.maxWidthForTerminal = maxw;
		}
		
		createAndSaveStaticRes(file, name, author, date, wallpaper);
		
		return wallpaper;
	}
	
	/**
	 * create wallpaper
	 * @return Wallpaper对象
	 * @throws IOException
	 */
	public static Wallpaper createWallpaper(File file, String name, String author, Set<Category> categories,
			int minw, int maxw, int minh, int maxh) throws IOException {
		Set<String> pkgForTerminals = new HashSet<String>();
		pkgForTerminals.add(SINGLE_PKG);
		pkgForTerminals.add(DOUBLE_PKG);
		
		Wallpaper wallpaper = createWallpaper(file, name, author, categories, pkgForTerminals, minw, maxw, minh, maxh);
		
		return wallpaper;
	}
	
	/**
	 * create wallpaper
	 * @return Wallpaper对象
	 * @throws IOException
	 */
	public static Wallpaper createWallpaper(File file, Set<Category> categories) throws IOException {
		Wallpaper wallpaper = createWallpaper(file, "手机壁纸", "来自互联网", categories, 0, 0, 0, 0);
		return wallpaper;
	}


	private static void createAndSaveStaticRes(File file, String name,
			String author, Date date, Wallpaper wallpaper) throws IOException {
		try {
			Properties conf = Play.configuration;
			String basePath = conf.getProperty("ftp.server.uploadBasePath");
			
			// 若图片大于200KB则压缩原图
			if (file.length() > 200 * 1024) {
				file = ImageUtils.compress(ImageIO.read(file), 80);
			}
			
			// 保存原图
			String originImgPath = basePath + ORIGIN_PREFIX.substring(1);
			String fileName = Store.generateRelativePathOnCdn(file, wallpaper.code, "", "_" + date.getTime());
			wallpaper.file = models.stores.Store.genUrl(Constants.S3_NOAUTH_BUCKET, originImgPath, fileName, false);//ORIGIN_PREFIX + fileName;
			if (!models.stores.Store.store(file, Constants.S3_NOAUTH_BUCKET, originImgPath, fileName, Qstore.SINGLE + "")) throw new RuntimeException("保存到CDN出错了");
			wallpaper.checksum = getChecksum(file);
			
			// 生成cover图
			File coverImg = ImageUtils.scale(file, COVER_WIDTH, COVER_HEIGHT);
			String coverName = Store.generateRelativePathOnCdn(coverImg, wallpaper.code, "", "_cover_" + date.getTime());
			wallpaper.cover = models.stores.Store.genUrl(Constants.S3_NOAUTH_BUCKET, originImgPath, coverName, false);//ORIGIN_PREFIX + coverName;
			if (!models.stores.Store.store(coverImg, Constants.S3_NOAUTH_BUCKET, originImgPath, coverName, Qstore.SINGLE + "")) throw new RuntimeException("保存到CDN出错了");
			
			// 生成各种缩略图
			String cropBaseImgPath = basePath + CROP_PREFIX.substring(1);
			String cropImgPath = "";
			for (int key : WALLPAPER_SIZE.keySet()) {
				File cropImg = ImageUtils.scale(file, key, WALLPAPER_SIZE.get(key));
				String cropName = Store.generateRelativePathOnCdn(cropImg, wallpaper.code, "", "_cover_" + date.getTime());
				cropImgPath = cropBaseImgPath + key + "x" + WALLPAPER_SIZE.get(key) + "/";
				if (!models.stores.Store.store(cropImg, Constants.S3_NOAUTH_BUCKET, cropImgPath, cropName, Qstore.SINGLE + "")) throw new RuntimeException("保存到CDN出错了");
			}
			
			// 生成zip包
			String zipPath = basePath + ZIPS_PREFIX.substring(1);
			File zip = BuildZip.buildWallpaper(name, author, file);
			String zipName = Store.generateRelativePathOnCdn(zip, wallpaper.code, "", "_" + date.getTime());
			if (!models.stores.Store.store(zip, Constants.S3_NOAUTH_BUCKET, zipPath, zipName, Qstore.SINGLE + "")) throw new RuntimeException("保存到CDN出错了");
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("file", models.stores.Store.genUrl(Constants.S3_NOAUTH_BUCKET, zipPath, zipName, false));
			map.put("checksum", getChecksum(zip));
			map.put("size", zip.length());
			wallpaper.info = new EasyMap<String, Object>("zip", map);
		} catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
	private static String getChecksum(File file) {
        try {
            return Long.toString(FileUtils.checksumCRC32(file));
        } catch (IOException e) {
            Logger.error("Generate checksum failed.", e);
            return null;
        }
    }
}
