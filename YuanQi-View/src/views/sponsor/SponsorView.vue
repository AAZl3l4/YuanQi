<script setup>
import { ref, onMounted, computed } from 'vue'
import { getSponsorList } from '@/api/sponsor'

const sponsorList = ref([])
const loading = ref(false)

const totalAmount = computed(() => {
  return sponsorList.value.reduce((sum, item) => sum + Number(item.amount), 0)
})

const getFirstChar = (name) => {
  return name ? name.charAt(0).toUpperCase() : '?'
}

const loadSponsorList = async () => {
  loading.value = true
  try {
    const res = await getSponsorList()
    if (res.code === 200) {
      sponsorList.value = res.data || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSponsorList()
})
</script>

<template>
  <div class="sponsor-view">
    <!-- 顶部标题 -->
    <div class="header">
      <h1 class="main-title">赞助榜单</h1>
      <div class="divider">
        <span class="line"></span>
        <span class="diamond"></span>
        <span class="line"></span>
      </div>
      <p class="subtitle">感谢每一位支持者，你们是平台前进的动力</p>
      <div class="stats">
        <div class="stat">
          <span class="stat-num">{{ sponsorList.length }}</span>
          <span class="stat-label">赞助人数</span>
        </div>
        <div class="stat-sep"></div>
        <div class="stat">
          <span class="stat-num">¥{{ totalAmount.toFixed(2) }}</span>
          <span class="stat-label">累计金额</span>
        </div>
      </div>
    </div>

    <!-- 领奖台前三名 -->
    <div class="podium-wrap" v-if="sponsorList.length > 0">
      <div class="podium">
        <!-- 第二名 -->
        <div v-if="sponsorList[1]" class="podium-item second">
          <div class="avatar-wrap">
            <div class="avatar silver">{{ getFirstChar(sponsorList[1].name) }}</div>
            <div class="rank-badge silver">2</div>
          </div>
          <div class="info">
            <div class="name">{{ sponsorList[1].name }}</div>
            <div v-if="sponsorList[1].adContent" class="ad">{{ sponsorList[1].adContent }}</div>
            <div class="amount">¥{{ Number(sponsorList[1].amount).toFixed(2) }}</div>
          </div>
        </div>

        <!-- 第一名 -->
        <div v-if="sponsorList[0]" class="podium-item first">
          <div class="crown">👑</div>
          <div class="avatar-wrap">
            <div class="avatar gold">{{ getFirstChar(sponsorList[0].name) }}</div>
            <div class="rank-badge gold">1</div>
          </div>
          <div class="info">
            <div class="name">{{ sponsorList[0].name }}</div>
            <div v-if="sponsorList[0].adContent" class="ad">{{ sponsorList[0].adContent }}</div>
            <div class="amount">¥{{ Number(sponsorList[0].amount).toFixed(2) }}</div>
          </div>
        </div>

        <!-- 第三名 -->
        <div v-if="sponsorList[2]" class="podium-item third">
          <div class="avatar-wrap">
            <div class="avatar bronze">{{ getFirstChar(sponsorList[2].name) }}</div>
            <div class="rank-badge bronze">3</div>
          </div>
          <div class="info">
            <div class="name">{{ sponsorList[2].name }}</div>
            <div v-if="sponsorList[2].adContent" class="ad">{{ sponsorList[2].adContent }}</div>
            <div class="amount">¥{{ Number(sponsorList[2].amount).toFixed(2) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 其他排名 -->
    <div class="list-wrap" v-if="sponsorList.length > 3">
      <div class="list-header">
        <span class="header-line"></span>
        <span class="header-text">更多赞助者</span>
        <span class="header-line"></span>
      </div>
      <div class="rank-list">
        <div
          v-for="(item, index) in sponsorList.slice(3)"
          :key="item.id"
          class="rank-row"
        >
          <div class="row-rank">{{ index + 4 }}</div>
          <div class="row-avatar">{{ getFirstChar(item.name) }}</div>
          <div class="row-info">
            <span class="row-name">{{ item.name }}</span>
            <span v-if="item.adContent" class="row-ad">{{ item.adContent }}</span>
          </div>
          <div class="row-amount">¥{{ Number(item.amount).toFixed(2) }}</div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="sponsorList.length === 0 && !loading" class="empty">
      <div class="empty-icon">✨</div>
      <p>暂无赞助记录，期待您的支持</p>
    </div>

    <!-- 底部 -->
    <div class="footer">
      ❤️ 感谢所有赞助者的支持 ❤️
    </div>
  </div>
</template>

<style scoped>
.sponsor-view {
  min-height: 100%;
  background: #faf8f5;
  padding-bottom: 40px;
}

/* ===== 顶部标题 ===== */
.header {
  text-align: center;
  padding: 40px 20px 28px;
}

.main-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(180deg, #d4a843 0%, #b8860b 50%, #8b6914 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 6px;
  margin: 0;
}

.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 12px 0;
}

.divider .line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #d4a843, transparent);
}

.divider .diamond {
  width: 8px;
  height: 8px;
  background: #d4a843;
  transform: rotate(45deg);
}

.subtitle {
  font-size: 14px;
  color: #c9a84c;
  margin: 0 0 20px 0;
}

.stats {
  display: inline-flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border: 1px solid #f0e6d0;
  border-radius: 8px;
  padding: 12px 28px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #b8860b;
}

.stat-label {
  font-size: 11px;
  color: #c9a84c;
}

.stat-sep {
  width: 1px;
  height: 28px;
  background: #f0e6d0;
}

/* ===== 领奖台 ===== */
.podium-wrap {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 24px;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  flex: 1;
  max-width: 220px;
}

.podium-item.first {
  order: 2;
  padding-bottom: 20px;
}

.podium-item.second {
  order: 1;
  padding-bottom: 40px;
}

.podium-item.third {
  order: 3;
  padding-bottom: 60px;
}

.crown {
  font-size: 32px;
  margin-bottom: 4px;
}

.avatar-wrap {
  position: relative;
  margin-bottom: 12px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f0e6d0, #e0d4c0);
}

.avatar.gold {
  width: 130px;
  height: 130px;
  font-size: 52px;
  background: linear-gradient(135deg, #ffd700, #daa520);
  border: 3px solid #ffd700;
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
}

.avatar.silver {
  background: linear-gradient(135deg, #e8e8e8, #c0c0c0);
  border: 2px solid #c0c0c0;
  box-shadow: 0 0 15px rgba(192, 192, 192, 0.3);
}

.avatar.bronze {
  background: linear-gradient(135deg, #daa520, #cd7f32);
  border: 2px solid #cd7f32;
  box-shadow: 0 0 15px rgba(205, 127, 50, 0.3);
}

.rank-badge {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  border: 2px solid #fff;
}

.rank-badge.gold {
  width: 36px;
  height: 36px;
  font-size: 18px;
  background: linear-gradient(135deg, #ffd700, #daa520);
}

.rank-badge.silver {
  background: linear-gradient(135deg, #e8e8e8, #a0a0a0);
  color: #555;
}

.rank-badge.bronze {
  background: linear-gradient(135deg, #daa520, #cd7f32);
}

.info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.first .name {
  font-size: 20px;
}

.ad {
  font-size: 12px;
  color: #999;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount {
  font-size: 22px;
  font-weight: 700;
  color: #b8860b;
}

.first .amount {
  font-size: 28px;
  color: #d4a000;
}

/* ===== 列表 ===== */
.list-wrap {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 20px 0;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.header-line {
  flex: 1;
  height: 1px;
  max-width: 100px;
  background: linear-gradient(90deg, transparent, #d4a843, transparent);
}

.header-text {
  font-size: 14px;
  color: #8b6914;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  border: 1px solid #f0ebe4;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  transition: all 0.2s ease;
}

.rank-row:hover {
  border-color: #d4a843;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.1);
}

.row-rank {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5efe3;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  color: #8b6914;
  flex-shrink: 0;
}

.row-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #d4a843, #b8860b);
  flex-shrink: 0;
}

.row-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.row-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.row-ad {
  font-size: 12px;
  color: #bbb;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-amount {
  font-size: 18px;
  font-weight: 700;
  color: #b8860b;
  flex-shrink: 0;
}

/* ===== 空状态 ===== */
.empty {
  text-align: center;
  padding: 80px 20px;
  color: #ccc;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

/* ===== 底部 ===== */
.footer {
  text-align: center;
  padding: 40px 20px 20px;
  color: #c4a35a;
  font-size: 14px;
}

/* ===== 响应式 ===== */
@media (max-width: 600px) {
  .main-title {
    font-size: 24px;
    letter-spacing: 3px;
  }

  .stats {
    padding: 10px 20px;
    gap: 16px;
  }

  .stat-num {
    font-size: 18px;
  }

  .podium {
    gap: 12px;
  }

  .avatar {
    width: 70px;
    height: 70px;
    font-size: 28px;
  }

  .avatar.gold {
    width: 90px;
    height: 90px;
    font-size: 36px;
  }

  .name {
    font-size: 14px;
  }

  .first .name {
    font-size: 16px;
  }

  .amount {
    font-size: 16px;
  }

  .first .amount {
    font-size: 20px;
  }

  .podium-item.second {
    padding-bottom: 20px;
  }

  .podium-item.third {
    padding-bottom: 40px;
  }
}
</style>