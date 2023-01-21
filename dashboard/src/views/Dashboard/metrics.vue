<template>
  <lay-row space="10">
    <lay-col md="8">
      <lay-table :columns="connect_columns" :data-source="connect_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>

    <lay-col md="8">
      <lay-table :columns="session_columns" :data-source="session_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>

    <lay-col md="8">
      <lay-table :columns="authorization_columns" :data-source="authorization_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>
  </lay-row>
  <lay-row>
    <lay-space direction="vertical" fill>
      消息传输
      <lay-line theme="red"></lay-line>
    </lay-space>
  </lay-row>
  <lay-row space="10">
    <lay-col md="8">
      <lay-table :columns="bytes_columns" :data-source="bytes_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>

    <lay-col md="8">
      <lay-table :columns="message_columns" :data-source="message_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>

    <lay-col md="8">
      <lay-table :columns="delivery_columns" :data-source="delivery_dataSource" :size="md" skin='nob'>
      </lay-table>
    </lay-col>
  </lay-row>

</template>

<script>

export default {
  setup() {
    const connect_columns = [
      {
        title:"连接",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const connect_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]

    const session_columns = [
      {
        title:"会话",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const session_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]

    const authorization_columns = [
      {
        title:"认证与权限",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const authorization_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]

    const bytes_columns = [
      {
        title:"流量收发（字节）",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const bytes_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]


    const message_columns = [
      {
        title:"消息数量",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const message_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]


    const delivery_columns = [
      {
        title:"消息分发",
        width:"200px",
        key:"connect"
      },{
        title:"",
        width: "180px",
        key:"value",
      }
    ]

    const delivery_dataSource = [
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
      {connect:"client.connack", value:"1"},
      {connect:"client.connect", value:"2"},
    ]
    return {
      connect_columns,
      connect_dataSource,
      session_columns,
      session_dataSource,
      authorization_columns,
      authorization_dataSource,
      bytes_columns,
      bytes_dataSource,
      message_columns,
      message_dataSource,
      delivery_columns,
      delivery_dataSource
    }
  }
}
</script>