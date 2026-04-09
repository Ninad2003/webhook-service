const fs = require('fs');
const path = require('path');

const outputFiles = ['ALL_CODE.txt', 'ALL_CODE.md'];
const directoriesToScan = ['backend', 'frontend', '.'];
const excludeDirs = ['node_modules', 'target', 'dist', '.git', '.idea', '.vscode', 'build', '.mvn'];
const excludeFiles = ['package-lock.json', 'ALL_CODE.txt', 'ALL_CODE.md', 'bundle.js'];
const allowedExtensions = ['.java', '.xml', '.properties', '.yml', '.yaml', '.js', '.jsx', '.css', '.html', '.md', '.json'];

// Helper to get extension without dot
function getExtName(filePath) {
    return path.extname(filePath).toLowerCase();
}

function getLangFromExt(ext) {
    if (ext === '.java') return 'java';
    if (ext === '.js' || ext === '.jsx') return 'javascript';
    if (ext === '.xml' || ext === '.html') return 'html';
    if (ext === '.properties') return 'properties';
    if (ext === '.yml' || ext === '.yaml') return 'yaml';
    if (ext === '.css') return 'css';
    if (ext === '.json') return 'json';
    if (ext === '.md') return 'markdown';
    return '';
}

function scanDir(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
            if (!excludeDirs.includes(file)) {
                scanDir(fullPath, fileList);
            }
        } else {
            if (
                !excludeFiles.includes(file) && 
                allowedExtensions.includes(getExtName(fullPath)) &&
                !fullPath.includes('node_modules') &&
                !fullPath.includes('target')
            ) {
                // If scanning '.' only include top level files that match, to avoid duplicates
                // Wait, since we explicitly scan 'backend' and 'frontend' first, let's just scan '.' and avoid overlaps, or just scan '.' recursively.
                // It's simpler to just scan '.' recursively and ignore the exclude dirs.
            }
        }
    }
    return fileList;
}

// Better approach: start at root '.'
function scanRoot(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            if (!excludeDirs.includes(file)) {
                scanRoot(fullPath, fileList);
            }
        } else {
            const ext = getExtName(file);
            if (!excludeFiles.includes(file) && allowedExtensions.includes(ext)) {
                fileList.push(fullPath);
            }
        }
    }
    return fileList;
}

const allFiles = scanRoot('.');
let outputContent = '';

for (const file of allFiles) {
    const absolutePath = path.resolve(file);
    const content = fs.readFileSync(file, 'utf8');
    const lang = getLangFromExt(getExtName(file));
    
    outputContent += `### File: ${absolutePath}\n`;
    outputContent += `\`\`\`${lang}\n`;
    outputContent += content;
    if (!content.endsWith('\n')) {
        outputContent += '\n';
    }
    outputContent += `\`\`\`\n\n`;
}

outputFiles.forEach(outFile => {
    fs.writeFileSync(path.join('.', outFile), outputContent, 'utf8');
    console.log(`Generated ${outFile} with ${allFiles.length} files.`);
});

console.log('Done!');
