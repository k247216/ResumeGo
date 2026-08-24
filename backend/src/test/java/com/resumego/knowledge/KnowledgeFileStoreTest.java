package com.resumego.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileStoreTest {

    @TempDir
    Path tempDir;

    private KnowledgeFileStore store() {
        return new KnowledgeFileStore(tempDir);
    }

    @Test
    void readsManagedFileAndRejectsSymlinkTraversalAndDirectory() throws Exception {
        Path target = tempDir.resolve("knowledge/sources/1/ok.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "old".getBytes(StandardCharsets.UTF_8));

        // 正常读取（回滚备份）
        assertThat(store().readManagedForReplace(1L, "knowledge/sources/1/ok.md"))
                .isEqualTo("old".getBytes(StandardCharsets.UTF_8));

        // 目录拒绝
        assertThatThrownBy(() -> store().readManagedForReplace(1L, "knowledge/sources/1"))
                .isInstanceOf(KnowledgeImportException.class);
        // 越界（../）
        assertThatThrownBy(() -> store().readManagedForReplace(1L, "knowledge/../secret.md"))
                .isInstanceOf(KnowledgeImportException.class);
        // 他人用户目录
        assertThatThrownBy(() -> store().readManagedForReplace(1L, "knowledge/sources/2/ok.md"))
                .isInstanceOf(KnowledgeImportException.class);
    }

    @Test
    void rejectsSymlinkManagedFile() throws Exception {
        Path target = tempDir.resolve("knowledge/sources/1/real.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "real".getBytes(StandardCharsets.UTF_8));
        Path link = tempDir.resolve("knowledge/sources/1/link.md");
        Files.createSymbolicLink(link, target);

        assertThatThrownBy(() -> store().readManagedForReplace(1L, "knowledge/sources/1/link.md"))
                .isInstanceOf(KnowledgeImportException.class);
    }

    @Test
    void stageCommitAndRestoreReplaceManagedFile() throws Exception {
        Path target = tempDir.resolve("knowledge/sources/1/ok.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "old".getBytes(StandardCharsets.UTF_8));
        KnowledgeFileStore store = store();

        Path staged = store.stageReplacement("new".getBytes(StandardCharsets.UTF_8));
        assertThat(staged).exists();
        store.commitReplacement(target, staged);
        assertThat(Files.readString(target)).isEqualTo("new");
        assertThat(staged).doesNotExist();

        store.restoreManaged(target, "old".getBytes(StandardCharsets.UTF_8));
        assertThat(Files.readString(target)).isEqualTo("old");
    }
}
