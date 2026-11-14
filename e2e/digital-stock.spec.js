import {test, expect} from '@playwright/test';
async function login(page) {
 await page.goto('/login');await page.getByLabel('E-mail',{exact:true}).fill('admin@level.local');
 await page.getByLabel('Senha',{exact:true}).fill('AdminLocal123!');await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await expect(page).not.toHaveURL(/\/login/);
}
test('stock adjustments and paid file/code delivery operate through the administration',async({page})=>{
 await login(page);
 await page.goto('/admin/produtos/novo');await page.getByLabel('Publicação',{exact:true}).selectOption('PUBLICADO');
 await page.getByLabel('Tipo de produto').selectOption('DIGITAL');
 await page.getByLabel('Nome *',{exact:true}).fill('Arquivo e2e privado');
 await page.getByLabel('SKU',{exact:true}).fill('DIGITAL-E2E');
 await page.getByLabel('Preço *',{exact:true}).fill('15');
 await page.getByLabel('Estoque *',{exact:true}).fill('2');
 await page.getByRole('button',{name:'Salvar produto'}).click();
 await expect(page).toHaveURL(/\/admin\/produtos$/);
 await page.goto('/admin/digitais');
 const option=page.locator('#digital-product option').filter({hasText:'Arquivo e2e privado'});
 const id=await option.getAttribute('value');await page.getByLabel('Produto',{exact:true}).selectOption(id);
 await page.getByRole('button',{name:'Gerenciar conteúdo'}).click();
 await page.getByLabel('Arquivo',{exact:true}).setInputFiles({name:'demo.txt',mimeType:'text/plain',buffer:Buffer.from('Private demo file')});
 await page.getByRole('button',{name:'Salvar arquivo'}).click();
 await expect(page.getByRole('status')).toContainText('Nova versão cadastrada');
 await page.goto('/admin/estoque?produtoId='+id);
 await page.getByLabel('Quantidade do ajuste').fill('3');await page.getByLabel('Motivo',{exact:true}).fill('Reposição para teste');
 await page.getByRole('button',{name:'Registrar ajuste'}).click();await expect(page.getByRole('status')).toContainText('Ajuste registrado');
 await page.screenshot({path:'test-results/stock-admin.png',fullPage:true});
 await page.goto('/admin/digitais');
 const license=await page.locator('#digital-product option').filter({hasText:'Código LEVEL'}).getAttribute('value');
 const token=decodeURIComponent((await page.context().cookies()).find(c=>c.name==='XSRF-TOKEN').value);
 const orderResponse=await page.request.post('/api/checkout/orders',{headers:{'X-XSRF-TOKEN':token},data:{
 idempotencyKey:'digital-e2e-checkout',nome:'Admin Teste',telefone:'11999999999',cep:'06400000',endereco:'Rua teste',numero:'10',bairro:'Centro',cidade:'Barueri',estado:'SP',pagamento:'pix',itens:[{produtoId:Number(id),quantidade:1},{produtoId:Number(license),quantidade:1}]}});
 expect(orderResponse.ok()).toBeTruthy();const order=await orderResponse.json();
 await page.goto('/conta/digitais');await expect(page.getByRole('link',{name:'Baixar arquivo'})).toHaveCount(0);
 await page.goto('/admin/pedidos/'+order.pedidoId);
 await page.getByRole('button',{name:'Simular pagamento aprovado'}).click();
 await page.goto('/conta/digitais');
 const file=page.getByRole('link',{name:'Baixar arquivo'});await expect(file).toBeVisible();
 const download=await page.request.get(await file.getAttribute('href'));expect(await download.text()).toBe('Private demo file');
 await page.getByRole('button',{name:'Ver código'}).click();await expect(page.locator('.lc-digital-code')).toContainText('DEMO-SEM-VALOR-');
 await page.screenshot({path:'test-results/digitals-customer.png',fullPage:true});
});
