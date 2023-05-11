package com.jrasp.agent.module.file.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.util.Map;

/**
 * 文件读取、写入、删除、遍历、重命名
 */
@MetaInfServices(Module.class)
@Information(id = "file-hook")
public class FileHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> requestInfoThreadLocal;

    private volatile Boolean disable = false;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        fileHook();
    }

    private static final String FILE = "file";

    private static final String FILE_READ = "file-read";

    private static final String FILE_LIST = "file-list";

    private static final String FILE_DELETE = "file-delete";

    private static final String FILE_UPLOAD = "file-upload";

    public void fileHook() {
        // 同一个EventWatchBuilder下的class进行一次匹配
        // 为了提高匹配效率
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * 读取文件字节流
                 * @see java.io.FileInputStream#FileInputStream(File)
                 * 验证 2022-12-08
                 */
                .onClass(new ClassMatcher("java/io/FileInputStream")
                        .onMethod("<init>(Ljava/io/File;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                File file = (File) advice.getParameterArray()[0];
                                algorithmManager.doCheck(FILE_READ, requestInfoThreadLocal.get(), file);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )
                /**
                 * 文件写入/上传
                 * @see java.io.FileOutputStream#FileOutputStream(File, boolean)
                 * 验证 2022-12-08
                 */
                .onClass(new ClassMatcher("java/io/FileOutputStream")
                        .onMethod("<init>(Ljava/io/File;Z)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                File file = (File) advice.getParameterArray()[0];
                                algorithmManager.doCheck(FILE_UPLOAD, requestInfoThreadLocal.get(), file);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )
                /**
                 * java/io/File
                 * open-rasp对于同一个类下多个方法的hook，类会转换多次，理论上不合理，这里进行了优化，一个类仅转换一次
                 */
                .onClass(new ClassMatcher("java/io/File")
                        /**
                         * 删除文件
                         * @see File#delete()
                         */
                        .onMethod("delete()Z", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                File file = (File) advice.getTarget();
                                algorithmManager.doCheck(FILE_DELETE, requestInfoThreadLocal.get(), file);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                        /**
                         * renameTo
                         */
                        .onMethod("renameTo(Ljava/io/File;)Z", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // issue: https://github.com/jvm-rasp/jrasp-agent/issues/17
                                algorithmManager.doCheck(FILE_UPLOAD, requestInfoThreadLocal.get(), advice.getParameterArray()[0]);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                        /**
                         * 文件遍历 list
                         * @see File#list()
                         */
                        .onMethod("list()[Ljava/lang/String;", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                File file = (File) advice.getTarget();
                                algorithmManager.doCheck(FILE_LIST, requestInfoThreadLocal.get(), file);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )
                /**
                 * 文件访问
                 * @see java/io/RandomAccessFile
                 */
                .onClass(new ClassMatcher("java/io/RandomAccessFile")
                        .onMethod("<init>(Ljava/io/File;Ljava/lang/String;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                File file = (File) advice.getParameterArray()[0];
                                String mode = (String) advice.getParameterArray()[1];
                                String type = "";
                                if (mode != null && mode.contains("rw")) {
                                    type = FILE_UPLOAD;
                                } else {
                                    type = FILE_READ;
                                }
                                algorithmManager.doCheck(type, requestInfoThreadLocal.get(), file);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )

                /**
                 * java nio jdk7以上
                 */
                .onClass(new ClassMatcher("java/nio/file/Files")
                        /**
                         * 读取文件
                         */
                        .onMethod(new String[]{
                                        "readAllBytes(Ljava/nio/file/Path;)[B",
                                        "newInputStream(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/InputStream;"
                                },
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        // Usage of API documented as @since 1.7+  忽略
                                        // jdk6上运行时如果匹配不到java.nio.file.Files类，也不会走到这里的逻辑，也就没有类加载不到的风险
                                        java.nio.file.Path filePath = (java.nio.file.Path) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(FILE_READ, requestInfoThreadLocal.get(), filePath.toFile());
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        requestInfoThreadLocal.remove();
                                    }
                                })
                        /**
                         * 文件上传
                         */
                        .onMethod(new String[]{
                                        "createFile(Ljava/nio/file/Path;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;",
                                        "newOutputStream(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/OutputStream;",
                                        "copy(Ljava/nio/file/Path;Ljava/nio/file/Path;[Ljava/nio/file/CopyOption;)Ljava/nio/file/Path;",
                                        "move(Ljava/nio/file/Path;Ljava/nio/file/Path;[Ljava/nio/file/CopyOption;)Ljava/nio/file/Path;"
                                },
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        // Usage of API documented as @since 1.7+  忽略
                                        // jdk6上运行时如果匹配不到java.nio.file.Files类，也不会走到这里的逻辑，也就没有类加载不到的风险
                                        java.nio.file.Path filePath = (java.nio.file.Path) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(FILE_UPLOAD, requestInfoThreadLocal.get(), filePath.toFile());
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        requestInfoThreadLocal.remove();
                                    }
                                })
                        /**
                         * 删除文件
                         */
                        .onMethod(new String[]{
                                        "delete(Ljava/nio/file/Path;)V",
                                        "deleteIfExists#(Ljava/nio/file/Path;)V"
                                },
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        // Usage of API documented as @since 1.7+  忽略
                                        // jdk6上运行时如果匹配不到java.nio.file.Files类，也不会走到这里的逻辑，也就没有类加载不到的风险
                                        java.nio.file.Path filePath = (java.nio.file.Path) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(FILE_DELETE, requestInfoThreadLocal.get(), filePath.toFile());
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        requestInfoThreadLocal.remove();
                                    }
                                })

                        /**
                         * 文件遍历
                         */
                        .onMethod(new String[]{
                                        "newDirectoryStream(Ljava/nio/file/Path;)Ljava/nio/file/DirectoryStream;",
                                        "newDirectoryStream(Ljava/nio/file/Path;Ljava/lang/String;)Ljava/nio/file/DirectoryStream;",
                                        "newDirectoryStream(Ljava/nio/file/Path;Ljava/nio/file/DirectoryStream$Filter;)Ljava/nio/file/DirectoryStream;"},
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        java.nio.file.Path filePath = (java.nio.file.Path) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(FILE_LIST, requestInfoThreadLocal.get(), filePath.toFile());
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        requestInfoThreadLocal.remove();
                                    }
                                })
                )
                /**
                 * springmvc @see org.springframework.web.multipart.support.StandardServletMultipartResolver
                 */
                .onClass(new ClassMatcher("org/apache/commons/fileupload/FileUploadBase")
                        .onMethod("parseRequest(Lorg/apache/commons/fileupload/RequestContext;)Ljava/util/List;", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // todo
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )
                // 第三方上传组件 common-fileupload
                // TODO 补充其他三方上传中间件 如：springmvc
                .onClass(new ClassMatcher("org/apache/commons/fileupload/FileUploadBase")
                        .onMethod("parseRequest(Lorg/apache/commons/fileupload/RequestContext;)Ljava/util/List;", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // todo
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestInfoThreadLocal.remove();
                            }
                        })
                )
                .build();
    }

}
