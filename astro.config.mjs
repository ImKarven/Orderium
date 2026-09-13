// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
    integrations: [
        starlight({
            title: 'Orderium',
            social: [
                { icon: 'github', label: 'GitHub', href: 'https://github.com/ImKarven/Orderium' },
            ],
            sidebar: [
                {
                    label: 'Getting Started',
                    items: [
                        { label: 'Installing', link: '/getting-started/installing' },
                        { label: 'Creating An Order', link: '/getting-started/creating-an-order' },
                    ],
                },
            ],
        }),
    ],
});
