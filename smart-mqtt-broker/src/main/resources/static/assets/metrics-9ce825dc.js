import{_ as f,g as m,a1 as S,o as g,j as w,J as c,I as t,F as x,ai as z,aj as b,C as o,D as a,K as D,ao as B,al as N,L as V,ar as j}from"./_plugin-vue_export-helper-a79656f4.js";/* empty css              */import"./index-2e34b8ae.js";/* empty css              *//* empty css              */import{b as C}from"./api-f6fb963d.js";const F={setup(){const l=[{title:"连接",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"80px",key:"value"}],p=m([]),k=[{title:"会话",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"180px",key:"value"}],s=m([]),h=[{title:"认证与权限",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"180px",key:"value"}],y=m([]),i=[{title:"报文",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"180px",key:"value"}],n=m([]),r=[{title:"消息数量",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"180px",key:"value"}],u=m([]),v=[{title:"消息分发",width:"200px",key:"metric",customSlot:"metric"},{title:"",width:"180px",key:"value"}],_=m([]);return S(()=>{(async()=>{const{data:d}=await C();console.log(d),p.value=d.group.connection,s.value=d.group.session,n.value=d.group.packet,y.value=d.group.access,u.value=d.group.message,_.value=d.group.delivery})()}),{connect_columns:l,connect_dataSource:p,session_columns:k,session_dataSource:s,packet_columns:i,packet_dataSource:n,access_columns:h,access_dataSource:y,message_columns:r,message_dataSource:u,delivery_columns:v,delivery_dataSource:_}}},E={class:"metric-cell"},I={class:"metric-cell"},J={class:"metric-cell"},K={class:"metric-cell"},L={class:"metric-cell"},M={class:"metric-cell"};function T(l,p,k,s,h,y){const i=B,n=N,r=z,u=V,v=j,_=b;return g(),w(x,null,[c(r,{space:"10"},{default:t(()=>[c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.connect_columns,"data-source":s.connect_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",E,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1}),c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.session_columns,"data-source":s.session_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",I,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1}),c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.access_columns,"data-source":s.access_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",J,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1})]),_:1}),c(r,null,{default:t(()=>[c(v,{direction:"vertical",fill:""},{default:t(()=>[D(" 消息传输 "),c(u,{theme:"red"})]),_:1})]),_:1}),c(_,{fluid:""},{default:t(()=>[c(r,{space:"10"},{default:t(()=>[c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.packet_columns,"data-source":s.packet_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",K,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1}),c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.message_columns,"data-source":s.message_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",L,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1}),c(n,{md:"8"},{default:t(()=>[c(i,{columns:s.delivery_columns,"data-source":s.delivery_dataSource,size:l.md,skin:"nob"},{metric:t(({data:e})=>[o("div",M,[o("p",null,a(e.code),1),o("span",null,a(e.desc),1)])]),_:1},8,["columns","data-source","size"])]),_:1})]),_:1})]),_:1})],64)}const Q=f(F,[["render",T]]);export{Q as default};