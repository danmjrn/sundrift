import {defineConfig,globalIgnores} from 'eslint/config';
import js from '@eslint/js';
import ts from 'typescript-eslint';
import hooks from 'eslint-plugin-react-hooks';
import next from '@next/eslint-plugin-next';
import globals from 'globals';
export default defineConfig([
  globalIgnores(['.next/**','playwright-report/**','test-results/**','next-env.d.ts']),
  js.configs.recommended,
  ...ts.configs.recommended,
  hooks.configs.flat.recommended,
  {languageOptions:{globals:{...globals.browser,...globals.node}},plugins:{'@next/next':next},rules:{...next.configs.recommended.rules,...next.configs['core-web-vitals'].rules}}
]);
