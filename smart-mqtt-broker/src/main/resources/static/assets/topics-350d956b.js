import{_,e as d,f as u,o as f,h as y,i as t,w as o,F as b,j as h,v as p,C as k,n as w,D as x,s as $}from"./index-ebe88bf1.js";/* empty css              *//* empty css              */import{e as v}from"./api-ce69e379.js";const D={setup(){const a=[{title:"主题",width:"120px",key:"topic"},{title:"节点",width:"80px",key:"brokerIpAddress"},{title:"操作",width:"180px",key:"ip"}],n=d([]);return u(()=>{(async()=>{const{data:e}=await v();console.log(e),n.value=e})()}),{columns2:a,dataSource2:n}}};function g(a,n,r,e,z,B){const i=k,s=w,c=x,l=h,m=$;return f(),y(b,null,[t(l,{space:"10"},{default:o(()=>[t(s,{sm:"6",md:"6"},{default:o(()=>[t(i,{placeholder:"主题"})]),_:1}),t(s,{sm:"6",md:"6"},{default:o(()=>[t(c,{"native-type":"submit"},{default:o(()=>[p("搜索")]),_:1}),t(c,{"native-type":"submit"},{default:o(()=>[p("刷新")]),_:1})]),_:1})]),_:1}),t(l,{space:"10"},{default:o(()=>[t(m,{columns:e.columns2,"data-source":e.dataSource2,size:a.md,skin:"nob"},null,8,["columns","data-source","size"])]),_:1})],64)}const V=_(D,[["render",g]]);export{V as default};