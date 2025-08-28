import { gray, green, magenta, red } from '@std/fmt/colors';

const { os } = Deno.build;

async function checkDep(name) {}

/**
 * uses `which` to determine if the command exists
 * @param {string} name
 * @returns boolean
 */
async function checkDepUnix(name) {
  const command = new Deno.Command('which', {
    args: ['-s', name],
  });

  const { code } = await command.output();
  return code === 0;
}

console.log(gray('Checking that extra build dependencies are installed...'));
const cargoExists = await checkDepUnix('cargo');
if (!cargoExists) {
  console.error(
    red(
      'Rust is required to build the credential interface with the OS in the desktop app (for storing your username + password set up with the file server).' +
        'Installation instructions can be found at https://rust-lang.org',
    ),
  );
  Deno.exit(1);
}
