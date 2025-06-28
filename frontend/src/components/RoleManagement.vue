<template>
  <div class="role-management-container">
    <h2 class="header">用户角色管理</h2>

    <div v-if="message" class="message" :class="{ error: !isSuccess }">
      {{ message }}
    </div>

    <div class="table-container">
      <table>
        <thead>
        <tr>
          <th>用户名</th>
          <th>手机号</th>
          <th>当前角色</th>
          <th>新角色</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="user in users" :key="user.phone">
          <td>{{ user.username }}</td>
          <td>{{ user.phone }}</td>
          <td>{{ user.role.roleName }}</td>
          <td>
            <select class="role-select" v-model="user.newRoleId">
              <option v-for="role in roles"
                      :value="role.roleId"
                      :selected="user.role.roleId === role.roleId">
                {{ role.roleName }}
              </option>
            </select>
          </td>
          <td>
            <button class="update-btn" @click="updateRole(user)">
              修改
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import {
  getAllUsersWithRoles,
  getAllRoles,
  updateUserRole
} from '@/service/roleService';

export default {
  data() {
    return {
      users: [],
      roles: [],
      message: '',
      isSuccess: false
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const [users, roles] = await Promise.all([
          getAllUsersWithRoles(),
          getAllRoles()
        ]);

        this.users = users.map(user => ({
          ...user,
          newRoleId: user.role.roleId
        }));
        this.roles = roles;
      } catch (error) {
        this.showMessage(false, error.message);
      }
    },
    async updateRole(user) {
      try {
        await updateUserRole(user.phone, user.newRoleId);
        // 更新本地数据
        const updatedRole = this.roles.find(r => r.roleId === user.newRoleId);
        user.role = updatedRole;
        this.showMessage(true, '角色更新成功');
      } catch (error) {
        this.showMessage(false, error.message);
      }
    },
    showMessage(isSuccess, message) {
      this.isSuccess = isSuccess;
      this.message = message;
      setTimeout(() => {
        this.message = '';
      }, 3000);
    }
  }
};
</script>

<style scoped>
/* 基础布局 - 与试卷页面保持一致 */
.role-management-container {
  display: flex;
  flex-direction: column;
  height: calc(90vh - 40px);
  padding: 20px;
  box-sizing: border-box;
  background-color: #f5f5f5;
  margin-left: 20px;
  overflow: hidden;
}

.header {
  color: #333;
  margin: 0;
  font-size: 22px;
  text-align: center;
  flex-shrink: 0;
}

/* 消息提示样式 */
.message {
  color: green;
  padding: 10px;
  margin: 10px 0;
  background-color: #e6ffe6;
  border-radius: 4px;
  flex-shrink: 0;
}

.error {
  color: red;
  background-color: #ffebee;
}

/* 表格容器 - 与试卷页面相同规格 */
.table-container {
  flex: 1;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  min-height: 160px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #ddd;
  background-image: linear-gradient(to bottom, #ddd, #ddd);
}

th, td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #dee2e6;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
}

tbody tr:nth-child(even) {
  background-color: #e6f7ff;
}

tbody tr:hover {
  background-color: #d8e6f7;
}

/* 表单元素样式 */
.role-select {
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.update-btn {
  padding: 6px 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.3s;
}

.update-btn:hover {
  background-color: #45a049;
}

/* 滚动条样式 - 与试卷页面一致 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .table-container {
    min-height: 120px;
  }
}
</style>