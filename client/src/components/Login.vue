<template>
  <div class="login-page">

    <div class="login-card">
      <h2>🔐 Đăng nhập hệ thống</h2>

      <input
        v-model="username"
        placeholder="Username"
      />

      <input
        v-model="password"
        type="password"
        placeholder="Password"
      />

      <button @click="login">
        Đăng nhập
      </button>

      <p v-if="message" class="msg">
        {{ message }}
      </p>
    </div>

  </div>
</template>

<script setup>
import { ref } from "vue"
import axios from "axios"
import { useRouter } from "vue-router"

const API_URL = "http://localhost:3000/auth"

const username = ref("")
const password = ref("")
const message = ref("")

const router = useRouter()

const login = async () => {
  try {

    const res = await axios.post(`${API_URL}/login`, {
      username: username.value,
      password: password.value
    })

    if (res.data === "Login success") {

      localStorage.setItem("login", "true")

      router.push("/app")

    } else {
      message.value = "❌ Sai tài khoản hoặc mật khẩu"
    }

  } catch (err) {
    message.value = "❌ Không kết nối được server"
  }
}
</script>

<style scoped>

.login-page{
  height:100vh;
  display:flex;
  justify-content:center;
  align-items:center;
  background:#f5f5f5;
  font-family:Arial, sans-serif;
}

.login-card{
  width:300px;
  padding:30px;
  background:white;
  border:1px solid #ddd;
}

.login-card h2{
  margin-bottom:20px;
  text-align:center;
  font-weight:500;
}

.login-card input{
  width:100%;
  padding:10px;
  margin-bottom:12px;
  border:1px solid #ccc;
  font-size:14px;
}

.login-card input:focus{
  outline:none;
  border-color:#999;
}

.login-card button{
  width:100%;
  padding:10px;
  border:1px solid #333;
  background:#333;
  color:white;
  cursor:pointer;
}

.login-card button:hover{
  background:#555;
}

.msg{
  margin-top:10px;
  color:red;
  font-size:14px;
  text-align:center;
}

</style>