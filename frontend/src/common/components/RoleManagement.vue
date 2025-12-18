<template>
  <div class="role-management-container">
    <div class="page-header">
      <h2 class="page-title">用户角色管理</h2>
    </div>

    <div v-if="message" class="message" :class="{ error: !isSuccess }">
      {{ message }}
    </div>

    <div class="table-container">
      <table class="data-table">
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
          <td><span class="role-badge">{{ user.role.roleName }}</span></td>
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
} from '@/common/service/roleService';

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
.role-management-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
  background-color: #f5f5f5;
  overflow: hidden;
}

.page-header {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.page-title {
  color: #333;
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

/* 消息提示样式 */
.message {
  padding: 12px 16px;
  margin: 0 0 16px 0;
  background-color: #f0f9ff;
  border-radius: 4px;
  border-left: 4px solid #4CAF50;
  flex-shrink: 0;
  color: #333;
  font-size: 14px;
}

.message.error {
  background-color: #fff5f5;
  border-left-color: #f44336;
  color: #d32f2f;
}

/* 表格容器 */
.table-container {
  flex: 1;
  overflow: auto;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  min-height: 400px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  background-color: #fff;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f8f9fa;
}

th {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  color: #333;
  font-weight: 600;
  background-color: #f8f9fa;
}

td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e9ecef;
  color: #333;
}

tbody tr {
  transition: background-color 0.2s;
}

tbody tr:nth-child(even) {
  background-color: #f9fafb;
}

tbody tr:hover {
  background-color: #f0f7ff;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

/* 表单元素样式 */
.role-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 100%;
  max-width: 200px;
  box-sizing: border-box;
  font-size: 14px;
  color: #333;
  background-color: #fff;
  transition: border-color 0.3s;
}

.role-select:focus {
  outline: none;
  border-color: #4CAF50;
}

.update-btn {
  padding: 8px 16px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
  font-weight: 500;
}

.update-btn:hover {
  background-color: #45a049;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 滚动条样式 */
.table-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>