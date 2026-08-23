package com.resumego.knowledge;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** 确定性严格 UTF-8 解码：非法字节抛 INVALID_UTF8。 */
public final class KnowledgeTextExtractor {

    private KnowledgeTextExtractor() {
    }


    /** DOCX 文本提取：JDK 内置 zip + XML，只取 word/document.xml 中的 <w:t> 文本，段落按 <w:p> 换行。 */
    public static String extractDocx(byte[] bytes) {
        try {
            var builder = new StringBuilder();
            try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!"word/document.xml".equals(entry.getName())) {
                        continue;
                    }
                    // 先整读该 entry 再解析：Xerces parse(InputStream) 会关闭流，不能直接解析 zip 流
                    byte[] xmlBytes = zip.readAllBytes();
                    var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(new java.io.ByteArrayInputStream(xmlBytes));
                    var paragraphs = doc.getElementsByTagName("w:p");
                    for (int i = 0; i < paragraphs.getLength(); i++) {
                        collectWt(paragraphs.item(i), builder);
                        builder.append('\n');
                    }
                    break;
                }
            }
            String text = builder.toString().trim();
            if (text.isEmpty()) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.EXTRACTION_FAILED, "Word 文档未包含可提取文本");
            }
            return text;
        } catch (KnowledgeImportException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.EXTRACTION_FAILED, "无法解析 Word 文档");
        }
    }

    private static void collectWt(org.w3c.dom.Node node, StringBuilder out) {
        if ("w:t".equals(node.getNodeName())) {
            out.append(node.getTextContent());
            return;
        }
        var children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectWt(children.item(i), out);
        }
    }

    public static String decodeUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_UTF8, "文件不是有效的 UTF-8 文本");
        }
    }
}
