import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StoreNameTagMatcher {

    public static void main(String[] args) {
        String inputFilePath = "F:\\Whui\\temp\\sample.csv";
        String dictionaryFilePath = "F:\\Whui\\temp\\标签词库1026.xlsx";
        String outputFilePath = "F:\\Whui\\temp\\output_with_tags.csv";

        matchAndAddTags(inputFilePath, dictionaryFilePath, outputFilePath);
    }

    private static void matchAndAddTags(String inputFilePath, String dictionaryFilePath, String outputFilePath) {
        try (CSVReader reader = new CSVReader(new FileReader(inputFilePath));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath));
             CSVReader dictionaryReader = new CSVReader(new FileReader(dictionaryFilePath))) {

            // 读取输入文件的头文件
            String[] inputHeader = reader.readNext();
            // 将新的头文件写入输出文件
            writer.writeNext(new String[]{inputHeader[1], inputHeader[4], "tag"});

            // 将字典读入一个集合以进行有效匹配
            Set<String> dictionary = new HashSet<>();
            String[] dictLine;
            while ((dictLine = dictionaryReader.readNext()) != null) {
                String keyword1 = dictLine[0];
                String keyword2 = dictLine[1];
                String keyword3 = dictLine[2];
                String tagValue = dictLine[3];

                dictionary.add(keyword1 + "|" + keyword2 + "|" + keyword3 + "|" + tagValue);
            }

            // 读取并处理输入文件的每一行
            String[] line;
            while ((line = reader.readNext()) != null) {
                String task_id = line[1];
                String data = line[4];

                // 从'data'字段中提取storeName(假设'data'是JSON格式)
                String storeName = extractStoreNameFromJson(data);

                // 匹配和添加标签
                String tag = matchTags(storeName, dictionary);

                // 将结果写入输出文件
                writer.writeNext(new String[]{task_id, storeName, tag});
            }

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    private static String extractStoreNameFromJson(String jsonData) {
        //实现从JSON数据中提取storeName的逻辑
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);

            // 获取data节点
            JsonNode dataNode = rootNode.path("data");

            // 获取data节点下的第一个元素
            JsonNode firstDataElement = dataNode.isArray() ? dataNode.get(0) : null;

            if (firstDataElement != null) {
                // 获取第一个元素下的data节点
                JsonNode innerDataNode = firstDataElement.path("data");

                // 获取innerDataNode下的storeName字段
                String storeName = innerDataNode.path("name").asText();

                return storeName;
            }
        } catch (Exception e) {
            e.printStackTrace(); // 在实际应用中，最好记录日志而不是打印堆栈跟踪
        }

        return null; // 如果发生异常或者数据格式不符合预期，返回null
    }

    private static String matchTags(String storeName, Set<String> dictionary) {
        for (String entry : dictionary) {
            String[] keywordsAndTag = entry.split("\\|");
            String keyword1 = keywordsAndTag[0];
            String keyword2 = keywordsAndTag[1];
            String keyword3 = keywordsAndTag[2];
            String tagValue = keywordsAndTag[3];

            if (StringUtils.containsIgnoreCase(storeName, keyword1) &&
                    StringUtils.containsIgnoreCase(storeName, keyword2) &&
                    StringUtils.containsIgnoreCase(storeName, keyword3)) {
                return tagValue;
            }
        }
        return ""; // No match found
    }
}

