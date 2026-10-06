import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const args = process.argv.slice(2);
const valueAfter = (flag) => {
  const index = args.indexOf(flag);
  return index >= 0 ? args[index + 1] : undefined;
};

const version = valueAfter('--version') || process.env.PLUGIN_VERSION;
const target = valueAfter('--target');
const mcpUrl = process.env.ZIP_GITHUB_MCP_URL || 'https://zip-github.apphome.one/mcp';
const chatgptAppId = process.env.ZIP_GITHUB_CHATGPT_APP_ID?.trim();
const allowedTargets = new Set(['chatgpt', 'desktop']);

if (!version || !/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/.test(version)) {
  throw new Error(`PLUGIN_VERSION must be a strict semantic version, got: ${version ?? '<missing>'}`);
}
if (!allowedTargets.has(target)) {
  throw new Error('Specify --target chatgpt or --target desktop');
}

const outputRoot = resolve(process.env.PLUGIN_OUTPUT_DIR || `build/plugin-${target}`);
const outputDir = resolve(outputRoot, 'zip-github');

await rm(outputRoot, { recursive: true, force: true });
await mkdir(outputDir, { recursive: true });

const pluginTemplate = await readFile('plugin/plugin.template.json', 'utf8');
const manifest = JSON.parse(pluginTemplate.replaceAll('__PLUGIN_VERSION__', version));

if (manifest.name !== 'zip-github' || manifest.version !== version) {
  throw new Error('Plugin identity/version validation failed');
}
if ((manifest.extensions?.['com.openai']?.interface?.shortDescription?.length ?? 0) > 30) {
  throw new Error('shortDescription exceeds 30 characters');
}

if (target === 'chatgpt') {
  if (!chatgptAppId) {
    throw new Error('ZIP_GITHUB_CHATGPT_APP_ID is required for the ChatGPT web package');
  }
  if (!/^(?:asdk_app_|connector_|templated_apps_)[A-Za-z0-9][A-Za-z0-9_-]*$/.test(chatgptAppId)) {
    throw new Error('ZIP_GITHUB_CHATGPT_APP_ID must be an app id such as asdk_app_..., not a plugin_... id');
  }

  const appTemplate = await readFile('plugin/app.template.json', 'utf8');
  const appJson = appTemplate.replaceAll('__CHATGPT_APP_ID__', chatgptAppId);
  const appManifest = JSON.parse(appJson);
  if (appManifest.apps?.['zip-github']?.id !== chatgptAppId || appManifest.apps?.['zip-github']?.required !== true) {
    throw new Error('Generated ChatGPT app binding validation failed');
  }

  manifest.extensions['com.openai'].apps = './.app.json';
  await writeFile(resolve(outputDir, '.app.json'), `${JSON.stringify(appManifest, null, 2)}\n`);
} else {
  const parsedUrl = new URL(mcpUrl);
  if (parsedUrl.protocol !== 'https:' || parsedUrl.pathname !== '/mcp') {
    throw new Error('ZIP_GITHUB_MCP_URL must be an HTTPS /mcp endpoint');
  }

  const mcpTemplate = await readFile('plugin/mcp.template.json', 'utf8');
  const mcpJson = mcpTemplate.replaceAll('__ZIP_GITHUB_MCP_URL__', mcpUrl);
  const mcp = JSON.parse(mcpJson);
  if (mcp.mcpServers?.['zip-github']?.url !== mcpUrl) {
    throw new Error('Generated MCP URL validation failed');
  }
  await writeFile(resolve(outputDir, 'mcp.json'), mcpJson);
}

await writeFile(resolve(outputDir, 'plugin.json'), `${JSON.stringify(manifest, null, 2)}\n`);
await cp('plugin/skills', resolve(outputDir, 'skills'), { recursive: true });

console.log(`Generated ${target} plugin package directory: ${outputDir}`);
console.log(`Plugin version: ${version}`);
if (target === 'chatgpt') {
  console.log(`ChatGPT app id: ${chatgptAppId}`);
} else {
  console.log(`MCP endpoint: ${mcpUrl}`);
}
