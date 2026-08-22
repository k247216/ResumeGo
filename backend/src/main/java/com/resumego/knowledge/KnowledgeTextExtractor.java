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
