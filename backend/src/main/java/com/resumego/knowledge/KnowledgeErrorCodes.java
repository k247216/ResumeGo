package com.resumego.knowledge;

/** 稳定的导入失败分类。400 校验失败与 FAILED job 的 error_code 均使用这些常量。 */
public final class KnowledgeErrorCodes {

    /** 文件名缺失或包含路径/控制字符。 */
    public static final String INVALID_FILENAME = "INVALID_FILENAME";
    /** 已识别但当前不支持解析/编辑的文件格式（如 pdf/doc/docx/unknown）。 */
    public static final String UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";
    /** 文件超过 10 MiB。 */
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    /** 读取上传字节失败。 */
    public static final String READ_FAILED = "READ_FAILED";
    /** staging 目录写入失败（此时不创建任何记录）。 */
    public static final String STAGING_FAILED = "STAGING_FAILED";
    /** 校验后原子移动失败（staging 副本保留）。 */
    public static final String COPY_FAILED = "COPY_FAILED";
    /** 字节不是合法 UTF-8（已移动的原始副本保留）。 */
    public static final String INVALID_UTF8 = "INVALID_UTF8";
    /** 文本提取过程其它失败（原始副本保留）。 */
    public static final String EXTRACTION_FAILED = "EXTRACTION_FAILED";

    private KnowledgeErrorCodes() {
    }
}
