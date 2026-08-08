import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const repositoryRoot = new URL('../', import.meta.url);

const surfaceScript = await readFile(
  new URL('app/src/main/assets/scripts/youtube_comments_surface.js', repositoryRoot),
  'utf8'
);
const labScript = await readFile(
  new URL('research/youtube-desktop-comments-lab/isolate-comments.js', repositoryRoot),
  'utf8'
);

function commentsRootStyle(script, label) {
  const match = script.match(/ytd-comments#comments\s*\{([\s\S]*?)\n\s*\}/);
  assert.ok(match, `${label} must style the isolated comments root`);
  return match[1];
}

test('app comments root has no vertical padding below continuations', () => {
  const style = commentsRootStyle(surfaceScript, 'app surface');
  assert.match(style, /padding:\s*0 12px !important;/);
  assert.doesNotMatch(style, /padding[^;]*48px/);
});

test('desktop extraction lab matches the app bottom-edge behavior', () => {
  const style = commentsRootStyle(labScript, 'desktop lab');
  assert.match(style, /padding:\s*0 16px !important;/);
  assert.doesNotMatch(style, /padding[^;]*48px/);
});
