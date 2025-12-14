package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.configuration.PhpDumpSettingsService
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class OpcodesDumperService(var project: Project) : DumperServiceInterface {
    val state = PhpDumpSettingsService.getInstance(project)

    override suspend fun dump(file: String): ProcessOutput {
        val debugLevel = state.debugLevel.value

        val localFile = PathMapper.map(project, file)
        val preloadFile = state.preloadFile.let {
            if (!it.isNullOrBlank()) { PathMapper.map(project, it) } else { "" }
        }

        // language=injectablephp
        val phpSnippet = $$"""
        if (!extension_loaded('opcache') && !extension_loaded('Zend OPcache')) {
            exit('Error: opcache extension is not loaded');
        }
        $file = $argv[1] ?? null;
        
        if (!is_string($file) || !is_file($file)) {
            exit('Error: Could not open input file "' . addcslashes(print_r($file, true), '"') . '"');
        }
        
        opcache_invalidate($file);
        opcache_compile_file($file);
        
        $iterations = 0;
        while (!opcache_is_script_cached($file) && $iterations++ < 10) {
            usleep(100);
            opcache_compile_file($file);
        }
        
        if (opcache_is_script_cached($file) === false) {
            exit('Error: Could not compile file "' . addcslashes(print_r($file, true), '"') . '"');
        }
        
        $temp = sys_get_temp_dir() . '/opcache_dump';
        
        if (!is_dir($temp)) {
            @mkdir($temp);
        }
        
        passthru(implode(' ', [
            PHP_BINARY, '-l',
            '-d', escapeshellarg('display_errors=0'),
            '-d', escapeshellarg('error_reporting=0'),
            
            '-d', escapeshellarg('opcache.file_cache_only=1'),
            '-d', escapeshellarg('opcache.file_cache=' . $temp),
            '-d', escapeshellarg('opcache.enable=1'),
            '-d', escapeshellarg('opcache.enable_cli=1'),
            '-d', escapeshellarg('opcache.save_comments=1'),
            '-d', escapeshellarg('opcache.opt_debug_level=$$debugLevel'),
            ...(!empty('$$preloadFile') ? ['-d', escapeshellarg('opcache.preload=$$preloadFile')] : []),
            escapeshellarg('$$localFile'),
        ]));
        
        $files = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($temp, FilesystemIterator::SKIP_DOTS),
            RecursiveIteratorIterator::CHILD_FIRST,
        );
        
        foreach ($files as $info) {
            if ($info->isDir()) {
                @rmdir($info->getRealPath());
            } else {
                @unlink($info->getRealPath());
            }
        }
        """.trimIndent()

//        println("command: $command")
//        println("phpSnippet: $phpSnippet")
        return withContext(Dispatchers.IO) {
            val output = ProcessOutput()

            PhpCommandExecutor.execute(
                localFile,
                phpSnippet,
                project,
                CapturingProcessAdapter(output),
                buildList {
                    addAll("-d display_errors=0".split(" "))
                    addAll("-d error_reporting=0".split(" "))

                    addAll("-d opcache.enable=1".split(" "))
                    addAll("-d opcache.enable_cli=1".split(" "))
                    addAll("-d opcache.save_comments=1".split(" "))
                },
            )

            output
        }
    }
}