<template>
  <div class="app">
    <h1>📸 Face ID – Hệ thống điểm danh</h1>

    <!-- Chọn chức năng -->
    <div class="menu">
      <button @click="openAttendance">✅ Điểm danh</button>
      <button @click="openAddStudent">➕ Thêm sinh viên</button>
    </div>

    <!-- Camera -->
    <div v-if="mode" class="camera-box">
      <video ref="video" autoplay playsinline class="camera"></video>
      <canvas ref="canvas" style="display:none"></canvas>

      <button class="capture" @click="capture">
        {{ mode === 'attendance' ? 'Quét & điểm danh' : 'Chụp & lưu khuôn mặt' }}
      </button>
    </div>

    <!-- Kết quả điểm danh -->
    <div v-if="attendanceResult && attendanceResult.success">
  <h3>📋 Kết quả điểm danh</h3>

  <p><b>Mã SV:</b> {{ attendanceResult.studentCode }}</p>
  <p><b>Họ tên:</b> {{ attendanceResult.fullName }}</p>
  <p><b>Lớp:</b> {{ attendanceResult.className }}</p>
  <p>
    <b>Độ giống:</b>
    {{ (attendanceResult.similarity * 100).toFixed(2) }}%
  </p>

  <p style="color: green">{{ attendanceResult.message }}</p>
</div>


    <!-- Form thêm sinh viên -->
    <div v-if="mode === 'add'" class="form">
      <h3>➕ Thông tin sinh viên</h3>
      <input v-model="newStudent.student_code" placeholder="Mã sinh viên" />
      <input v-model="newStudent.full_name" placeholder="Họ tên" />
      <input v-model.number="newStudent.age" placeholder="Tuổi" />
      <input v-model="newStudent.class_name" placeholder="Lớp" />
    </div>

    <p v-if="message" class="msg">{{ message }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const video = ref(null)
const canvas = ref(null)
const mode = ref(null) // 'attendance' | 'add'

const attendanceResult = ref(null)
const message = ref('')

// ✅ DÙNG CAMELCASE CHO ĐỒNG BỘ BACKEND
const newStudent = ref({
  studentCode: '',
  fullName: '',
  age: null,
  className: ''
})

/* ================= CAMERA ================= */

const startCamera = async () => {
  const stream = await navigator.mediaDevices.getUserMedia({ video: true })
  video.value.srcObject = stream
}

const openAttendance = async () => {
  mode.value = 'attendance'
  attendanceResult.value = null
  message.value = ''
  await startCamera()
}

const openAddStudent = async () => {
  mode.value = 'add'
  attendanceResult.value = null
  message.value = ''
  await startCamera()
}

/* ================= CAPTURE ================= */

const capture = async () => {
  const ctx = canvas.value.getContext('2d')
  canvas.value.width = video.value.videoWidth
  canvas.value.height = video.value.videoHeight
  ctx.drawImage(video.value, 0, 0)

  const imageBase64 = canvas.value.toDataURL('image/jpeg')

  try {
    // ======= ĐIỂM DANH =======
    if (mode.value === 'attendance') {
      const res = await axios.post('http://localhost:3000/api/attendance', {
        image: imageBase64
      })

      console.log('ATTENDANCE RESPONSE:', res.data)
      attendanceResult.value = res.data
    }

    // ======= THÊM SINH VIÊN =======
    if (mode.value === 'add') {
      await axios.post(
        'http://localhost:3000/api/studentface/register-with-face',
        {
          studentCode: newStudent.value.studentCode,
          fullName: newStudent.value.fullName,
          age: newStudent.value.age,
          className: newStudent.value.className,
          images: [imageBase64] // ✅ BẮT BUỘC LÀ MẢNG
        }
      )

      message.value = '✅ Đã thêm sinh viên & khuôn mặt'
    }
  } catch (err) {
    console.error(err)
    message.value =
      err.response?.data?.message || '❌ Có lỗi xảy ra'
  }
}
</script>


<style scoped>
.app {
  max-width: 600px;
  margin: auto;
  font-family: Arial, sans-serif;
  text-align: center;
}
.menu button {
  margin: 10px;
  padding: 10px 20px;
  font-size: 16px;
}
.camera-box {
  margin-top: 20px;
}
.camera {
  width: 100%;
  border-radius: 8px;
  border: 2px solid #333;
}
.capture {
  margin-top: 10px;
  padding: 10px 20px;
}
.form input {
  display: block;
  width: 100%;
  margin: 6px 0;
  padding: 8px;
}
.result {
  background: #e6ffe6;
  margin-top: 15px;
  padding: 10px;
  border-radius: 6px;
}
.msg {
  color: green;
  margin-top: 10px;
}
</style>