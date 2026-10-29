import { defineConfig } from '@playwright/test';
export default defineConfig({testDir:'e2e',workers:1,retries:0,timeout:90000,use:{baseURL:'http://localhost:8080',viewport:{width:1440,height:1000},screenshot:'only-on-failure',trace:'retain-on-failure'}});
