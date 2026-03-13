<template>
  <div class="app">
    <h1>📸 Face ID – Hệ thống điểm danh</h1>

    <!-- MENU -->
    <div class="menu">
      <button @click="openAttendance">✅ Điểm danh</button>
      <button @click="openAddStudent">➕ Thêm SV</button>
      <button @click="openHistory">📅 Lịch sử</button> <!-- Nút mới -->
    </div>

    <!-- 1. MÀN HÌNH CAMERA (Điểm danh & Thêm SV) -->
    <div v-if="mode === 'attendance' || mode === 'add'" class="camera-wrapper">
      <video ref="video" autoplay playsinline class="camera"></video>
      <canvas ref="canvas" class="overlay"></canvas>

      <button class="capture" @click="capture">
        {{ mode === 'attendance' ? 'Quét & điểm danh' : 'Chụp & lưu khuôn mặt' }}
      </button>
    </div>

    <!-- KẾT QUẢ ĐIỂM DANH -->
    <!-- Thêm class binding để đổi màu nền -->
    <div v-if="attendanceResult"
     class="result"
     :class="{
       'warning-bg': attendanceResult.alreadyCheckedIn,
       'error-bg': !attendanceResult.success
     }"
>
  <h3>
    {{
      !attendanceResult.success
        ? '❌ Không nhận diện được'
        : attendanceResult.alreadyCheckedIn
        ? '⚠️ Đã điểm danh rồi'
        : '✅ Kết quả điểm danh'
    }}
  </h3>

  <template v-if="attendanceResult.success">
    <p><b>Mã SV:</b> {{ attendanceResult.studentCode }}</p>
    <p><b>Họ tên:</b> {{ attendanceResult.fullName }}</p>
    <p><b>Lớp:</b> {{ attendanceResult.className }}</p>
    <p>
      <b>Độ giống:</b>
      {{ (attendanceResult.similarity * 100).toFixed(2) }}%
    </p>
  </template>

  <p :style="{
      color: attendanceResult.alreadyCheckedIn
        ? '#d9534f'
        : attendanceResult.success
        ? 'green'
        : 'red',
      fontWeight: 'bold'
    }"
  >
    {{ attendanceResult.message }}
  </p>
</div>

    <!-- FORM THÊM SINH VIÊN -->
    <div v-if="mode === 'add'" class="form">
      <h3>➕ Thông tin sinh viên</h3>
      <input v-model="newStudent.studentCode" placeholder="Mã sinh viên" />
      <input v-model="newStudent.fullName" placeholder="Họ tên" />
      <input v-model.number="newStudent.age" placeholder="Tuổi" />
      <input v-model="newStudent.className" placeholder="Lớp" />
    </div>

    <!-- 2. MÀN HÌNH LỊCH SỬ (MỚI THÊM) -->
    <div v-if="mode === 'history'" class="history-section">
      <h3>📅 Lịch sử điểm danh</h3>
      
      <div class="filter-box">
        <!-- Lọc theo Ngày -->
        <div class="filter-group">
          <label>Theo Ngày:</label>
          <input type="date" v-model="selectedDate" />
          <button @click="fetchHistoryByDate">🔍 Xem Ngày</button>
        </div>
          <!-- loc theo thang -->
         <div class="filter-group">
          <label>Theo Tháng:</label>
          <input type="month" v-model="selectedMonth" />
          <button @click="fetchHistoryByMonth" class="btn-month">📅 Xem Tháng</button>
        </div>
      </div>

      <!-- Bảng dữ liệu -->
      <table class="styled-table" v-if="historyList.length > 0">
        <thead>
          <tr>
            <th>STT</th>
            <th>Ngày</th>
            <th>Mã SV</th>
            <th>Họ Tên</th>
            <th>Lớp</th>
            <th>Giờ vào</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in historyList" :key="item.id">
            <td>{{ index + 1 }}</td>
            <!-- Hiển thị ngày (DD/MM) -->
            <td>{{ new Date(item.checkInTime).toLocaleDateString('vi-VN') }}</td>
            <td>{{ item.studentCode }}</td>
            <td>{{ item.student?.fullName || '---' }}</td>
            <td>{{ item.student?.className || '---' }}</td>
            <td>{{ formatTime(item.checkInTime) }}</td>
          </tr>
        </tbody>
      </table>

      <p v-else style="color: gray; margin-top: 20px;">
        Chưa có dữ liệu điểm danh nào trong ngày này.
      </p>
    </div>

    <!-- THÔNG BÁO CHUNG -->
    <p v-if="message" class="msg">{{ message }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import * as faceapi from 'face-api.js'

/* ================== CONFIG URL BACKEND ================== */
// Lưu ý: Backend Spring Boot mặc định chạy cổng 3000
const API_URL = 'http://localhost:3000/api' 

/* ================== STATE ================== */
const video = ref(null)
const canvas = ref(null)
const mode = ref(null) // 'attendance', 'add', 'history'

const attendanceResult = ref(null)
const message = ref('')

// State cho thêm sinh viên
const newStudent = ref({
  studentCode: '',
  fullName: '',
  age: null,
  className: ''
})

// State cho lịch sử (MỚI)
const selectedDate = ref(new Date().toISOString().split('T')[0]) // Lấy ngày hôm nay yyyy-mm-dd
const selectedMonth = ref(new Date().toISOString().slice(0, 7)) // Mặc định tháng hiện tại (YYYY-MM)
const historyList = ref([])

let detectInterval = null

/* ================== LOAD MODEL ================== */
const loadModels = async () => {
  await faceapi.nets.tinyFaceDetector.loadFromUri('/models')
}

onMounted(async () => {
  await loadModels()
})

/* ================== CAMERA LOGIC ================== */
const startCamera = async () => {
  const stream = await navigator.mediaDevices.getUserMedia({ video: true })
  video.value.srcObject = stream
  video.value.onloadedmetadata = () => {
    video.value.play()
    if (detectInterval) clearInterval(detectInterval)
    detectInterval = setInterval(detectFace, 150)
  }
}

const stopCamera = () => {
  if (detectInterval) clearInterval(detectInterval)
  if (video.value?.srcObject) {
    video.value.srcObject.getTracks().forEach(t => t.stop())
  }
}

/* ================== CHUYỂN ĐỔI MODE ================== */
const openAttendance = async () => {
  stopCamera()
  mode.value = 'attendance'
  attendanceResult.value = null
  message.value = ''
  await startCamera()
}

const openAddStudent = async () => {
  stopCamera()
  mode.value = 'add'
  attendanceResult.value = null
  message.value = ''
  await startCamera()
}

// Hàm mở lịch sử (MỚI)
const openHistory = () => {
  stopCamera()
  mode.value = 'history'
  attendanceResult.value = null
  message.value = ''
  fetchHistoryByDate() // Mặc định load ngày hôm nay
}

/* ================== LOGIC LỊCH SỬ ================== */
const fetchHistoryByDate = async () => {
  try {
    const res = await axios.get(`${API_URL}/history`, {
      params: { date: selectedDate.value }
    })
    historyList.value = res.data
  } catch (err) {
    message.value = '❌ Lỗi tải dữ liệu ngày'
  }
}

const fetchHistoryByMonth = async () => {
  try {
    // selectedMonth có dạng "2023-10" -> Cần tách ra
    const [year, month] = selectedMonth.value.split('-')
    
    const res = await axios.get(`${API_URL}/history/month`, {
      params: { 
        month: month,
        year: year
      }
    })
    historyList.value = res.data
    
    if(res.data.length === 0) {
        message.value = "⚠️ Tháng này chưa có dữ liệu nào."
    } else {
        message.value = `✅ Tìm thấy ${res.data.length} bản ghi trong tháng ${month}/${year}`
    }

  } catch (err) {
    console.error(err)
    message.value = '❌ Lỗi tải dữ liệu tháng'
  }
}


const formatTime = (isoString) => {
  if (!isoString) return ''
  return new Date(isoString).toLocaleTimeString('vi-VN')
}

/* ================== FACE DETECT ================== */
const detectFace = async () => {
  if (!video.value || video.value.readyState !== 4) return

  const width = video.value.videoWidth
  const height = video.value.videoHeight
  canvas.value.width = width
canvas.value.height = height

  const detections = await faceapi.detectAllFaces(
    video.value,
    new faceapi.TinyFaceDetectorOptions({ 
      inputSize: 320, 
      scoreThreshold: 0.5 
    })
  )

  const resized = faceapi.resizeResults(detections, { width, height })

  const ctx = canvas.value.getContext('2d')
  ctx.clearRect(0, 0, width, height)

  // 👉 Vẽ khung thủ công (KHÔNG có %)
  resized.forEach(detection => {
    const { x, y, width, height } = detection.box

    ctx.beginPath()
    ctx.rect(x, y, width, height)
    ctx.lineWidth = 3
    ctx.strokeStyle = '#00ff00'
    ctx.stroke()
  })
}

/* ================== CAPTURE & SUBMIT ================== */
const capture = async () => {

  attendanceResult.value = null; 
  message.value = "⏳ Đang xử lý..."; 

  const ctx = canvas.value.getContext('2d')
  ctx.drawImage(video.value, 0, 0)
  const imageBase64 = canvas.value.toDataURL('image/jpeg')

  try {
    if (mode.value === 'attendance') {
      const res = await axios.post(`${API_URL}/attendance`, { image: imageBase64 })
      
      attendanceResult.value = res.data;
      message.value = ""; // Xóa dòng "Đang xử lý"
    }

    if (mode.value === 'add') {
      await axios.post(`${API_URL}/studentface/register-with-face`, {
        studentCode: newStudent.value.studentCode,
        fullName: newStudent.value.fullName,
        age: newStudent.value.age,
        className: newStudent.value.className,
        images: [imageBase64]
      })
      message.value = '✅ Đã thêm sinh viên & khuôn mặt'
    }
  } catch (err) {
    message.value = '❌ Có lỗi xảy ra hoặc không kết nối được Server';
    attendanceResult.value = null; // Xóa bảng kết quả nếu lỗi
  }
}
</script>

<style scoped>
.app {
  max-width: 800px; /* Tăng chiều rộng để hiển thị bảng đẹp hơn */
  margin: auto;
  font-family: Arial, sans-serif;
  text-align: center;
  padding-bottom: 50px;
}

.menu button {
  margin: 10px;
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
}
.menu button:hover {
  background-color: #0056b3;
}

.camera-wrapper {
  position: relative;
  margin-top: 20px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.camera {
  width: 100%;
  border-radius: 8px;
  border: 2px solid #333;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
}

.capture {
  margin-top: 10px;
  padding: 12px 24px;
  background-color: #28a745;
  color: white;
  font-size: 16px;
  border: none;
  cursor: pointer;
  border-radius: 5px;
}

.form input {
  display: block;
  width: 80%;
  margin: 10px auto;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.result {
  background: #e6ffe6; /* Xanh nhạt */
  margin-top: 15px;
  padding: 10px;
  border-radius: 6px;
  border: 2px solid #b3ffb3;
}

.warning-bg {
  background: #fff3cd !important; /* Vàng nhạt */
  border-color: #ffeeba !important;
}

.msg {
  color: #d9534f;
  margin-top: 15px;
  font-weight: bold;
}

/* CSS CHO PHẦN LỊCH SỬ */
.history-section {
  margin-top: 20px;
}
.filter-box {
  margin-bottom: 15px;
}
.filter-box input {
  padding: 8px;
  margin-right: 10px;
}

.styled-table {
  width: 100%;
  border-collapse: collapse;
  margin: 25px 0;
  font-size: 0.9em;
  font-family: sans-serif;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);
}
.styled-table thead tr {
  background-color: #009879;
  color: #ffffff;
  text-align: left;
}
.styled-table th,
.styled-table td {
  padding: 12px 15px;
  border-bottom: 1px solid #dddddd;
}
.styled-table tbody tr:nth-of-type(even) {
  background-color: #f3f3f3;
}
.styled-table tbody tr:last-of-type {
  border-bottom: 2px solid #009879;
}
.filter-group {
  display: inline-block;
  margin: 10px;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.filter-group label {
  font-weight: bold;
  margin-right: 5px;
}

.btn-month {
  background-color: #17a2b8; /* Màu xanh dương nhạt cho nút tháng */
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}


</style>