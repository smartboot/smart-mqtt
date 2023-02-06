import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import {LayuiVueResolver} from "unplugin-layui-vue-resolver";
import {visualizer} from "rollup-plugin-visualizer";

const excludeComponents = ['LightIcon','DarkIcon']

export default defineConfig({
  base:'./',
  server:{
    proxy:{
      '/api': {
        target: 'http://82.157.162.230:8083/api/',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      }
    }
  },
  plugins: [
    AutoImport({
      resolvers: [
        LayuiVueResolver(),
      ],
    }),
    Components({
      resolvers: [
        LayuiVueResolver({
          resolveIcons: true,
          exclude: excludeComponents
        }),
      ],
    }),
    visualizer({
      emitFile: true,
      filename: "visualizer.html",
      gzipSize: true,
      brotliSize: true,
      open: true,
    }),
    vue(),
  ],
});