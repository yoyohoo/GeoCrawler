package com.example.mybatis.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.mybatis.entity.Region;
import com.example.mybatis.service.RegionService;
import org.apache.tomcat.jni.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.*;
import java.io.*;
import java.util.List;

@RestController
@SpringBootApplication
@RequestMapping(value = "/Crawler")
public class CrawlerController {

    @Autowired
    private RegionService regionService;

    //    爬取控制相关
    private boolean hasChildRegion;

    //    爬取地址相关
    private String crawlerAddress = "https://geo.datav.aliyun.com/areas/bound/";
    private String crawlerExtention = "_full";
    private String crawlerDestination = "D:\\ZXF\\GEO\\";
    private String fileExtention = ".json";
    private String fileIndexName = "\\index.json";

    //    连接相关
    private Proxy proxy;
    private HttpURLConnection conn;
    private InputStream inStream;

    @CrossOrigin
    @RequestMapping(value = "/Craw/{id}")
    public void Craw(@PathVariable Integer id) {

        crawlerDestination = "D:\\Users\\zhuxianfu047\\Desktop\\GEO\\";

        List<Region> regionProvinceList = null;
        String regionDestination = null;
        if (id == 0) {
            regionDestination = crawlerDestination;
            regionProvinceList = regionService.getAllProvinces();
        } else {
            regionProvinceList = regionService.getCityByProvince(id);
            regionDestination = crawlerDestination + id + "\\";
        }
        File regionFile = new File(regionDestination);
        if (!regionFile.exists()) {
            regionFile.mkdirs();
        }
        CrawRegionIndex(regionFile, regionProvinceList);

        for (Integer i = 0; i < regionProvinceList.size(); i++) {
            hasChildRegion = true;
            Region regionProvince = regionProvinceList.get(i);
            File folderProvince = new File(regionDestination + regionProvince.getId());
            if (!folderProvince.exists()) {
                folderProvince.mkdirs();
            }
            File fileProvince = getCrawFile(folderProvince, regionProvince);
            CrawRegion(getCrawlerAddress(regionProvince), fileProvince);

            List<Region> regionCityList = regionService.getCityByProvince(regionProvince.getId());
            CrawRegionIndex(folderProvince, regionCityList);
            for (Integer j = 0; j < regionCityList.size(); j++) {
                hasChildRegion = true;
                Region regionCity = regionCityList.get(j);
                File folderCity = new File(folderProvince.getPath() + "\\" + regionCity.getId());
                if (!folderCity.exists()) {
                    folderCity.mkdirs();
                }
                File fileCity = getCrawFile(folderCity, regionCity);
                CrawRegion(getCrawlerAddress(regionCity), fileCity);

                List<Region> regionCountryList = regionService.getCountryByCity(regionCity.getId());
                CrawRegionIndex(folderCity, regionCountryList);
                for (Integer k = 0; k < regionCountryList.size(); k++) {
                    hasChildRegion = false;
                    Region regionCountry = regionCountryList.get(k);
                    File fileCountry = getCrawFile(folderCity, regionCountry);
                    CrawRegion(getCrawlerAddress(regionCountry), fileCountry);
                }
            }
        }
    }


    //    生产完整写入文件地址
    private File getCrawFile(File folder, Region region) {
        return new File(folder.getPath() + "\\" + region.getId() + fileExtention);
    }

    //    获取完整下载文件地址
    private String getCrawlerAddress(Region region) {
        return crawlerAddress + region.getId() + (hasChildRegion ? crawlerExtention : "") + fileExtention;
    }

    //    生成编码表
    private void CrawRegionIndex(File folder, List<Region> list) {
        File file = new File(folder.getPath() + fileIndexName);
        if (file.exists()) {
            file.delete();
        }
        JSONObject jsonObject = new JSONObject(true);
        for (Integer i = 0; i < list.size(); i++) {
            Region region = list.get(i);
            jsonObject.put(region.getId().toString(), region.getName());
        }
        BufferedWriter bufferedWriter = null;
        try {
            OutputStream outputStream = new FileOutputStream(file, false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(jsonObject.toJSONString());
            bufferedWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //爬取区域
    private void CrawRegion(String address, File file) {
        System.out.println(address + " - " + file.getPath().toString());
        try {
            URL url = new URL(address);
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.17.171.129", 8080));
//            URL url= new URL(null, address, new sun.net.www.protocol.https.Handler());
            conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko");
            conn.connect();
            Thread.sleep(1000);
            inStream = conn.getInputStream();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, len);
            }
            inStream.close();
            outStream.close();
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream op = new FileOutputStream(file);
            op.write(outStream.toByteArray());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

}
