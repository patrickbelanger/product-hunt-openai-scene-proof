import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const executable = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
const child = spawn(executable, ['-jar', resolve('apps/api/build/libs/api-0.1.0.jar')], {
  stdio: 'inherit', windowsHide: true, env: { ...process.env, MEDIA_ROOT: process.env.MEDIA_ROOT ?? resolve('.local/media') },
});
child.on('error', () => { console.error('Unable to launch the API. Check Java 25 and build the API jar first.'); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code ?? 1; });
process.on('SIGINT', () => child.kill());
process.on('SIGTERM', () => child.kill());
