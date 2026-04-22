<template>
  <section v-if="report" class="dashboard-shell">
    <div class="dashboard-container">
      <div class="header-section">
        <div class="header-left">
          <div class="subtitle">化验异常图谱</div>
          <h2 class="title">单张化验单异常概览</h2>
        </div>
        <div class="header-stats">
          <div class="stat-box">
            <div class="stat-label">总指标</div>
            <div class="stat-value">{{ report.totalItems || 0 }}</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">异常指标</div>
            <div class="stat-value alert">{{ report.abnormalCount || 0 }}</div>
          </div>
        </div>
      </div>

      <div v-if="sortedAbnormalItems.length" class="main-layout">
        <aside class="panel-left">
          <h3 class="panel-title">异常指标列表</h3>
          <div class="marker-list">
            <button
              v-for="(item, index) in sortedAbnormalItems"
              :key="`${item.item}-${index}`"
              class="marker-item"
              :class="{ active: index === selectedIndex }"
              type="button"
              @click="selectedIndex = index"
            >
              <span class="marker-header">
                <span class="marker-name">{{ item.item }}</span>
                <span class="marker-status" :class="statusClass(item.direction)">
                  {{ shortStatus(item.direction) }} {{ item.deviationLabel || scoreLabel(item.deviationScore) }}
                </span>
              </span>
              <span class="marker-value">
                {{ item.value }}<span v-if="item.unit"> {{ item.unit }}</span>
                <span class="range-text">参考 {{ item.normalRange || item.referenceRange || "未识别" }}</span>
              </span>
            </button>
          </div>
        </aside>

        <section class="panel-center">
          <h3 class="panel-title">历史趋势与参考范围 - {{ selectedItem.item }}</h3>

          <div class="trend-card">
            <div class="trend-summary">
              <div>
                <span>2025年11月</span>
                <strong>{{ valueLabel(historySeries[0]) }}</strong>
                <small>较重发热期</small>
              </div>
              <div>
                <span>2026年1月</span>
                <strong>{{ valueLabel(historySeries[1]) }}</strong>
                <small>轻微感冒期</small>
              </div>
              <div class="current">
                <span>本次</span>
                <strong>{{ valueLabel(historySeries[2]) }}</strong>
                <small>{{ shortStatus(selectedItem.direction) }}</small>
              </div>
            </div>

            <div class="chart-box">
              <svg viewBox="0 0 640 280" role="img" :aria-label="`${selectedItem.item} 历史趋势`">
                <defs>
                  <linearGradient id="trendFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stop-color="#8f82f1" stop-opacity="0.20" />
                    <stop offset="100%" stop-color="#8f82f1" stop-opacity="0.02" />
                  </linearGradient>
                </defs>

                <g class="grid-lines">
                  <line v-for="y in gridYs" :key="y" x1="54" x2="610" :y1="y" :y2="y" />
                </g>

                <line class="limit high" x1="54" x2="610" :y1="yFor(selectedItem.uln)" :y2="yFor(selectedItem.uln)" />
                <text class="limit-label high" x="612" :y="yFor(selectedItem.uln) - 4">上限 {{ selectedItem.uln ?? "--" }}</text>

                <line class="limit low" x1="54" x2="610" :y1="yFor(selectedItem.lln)" :y2="yFor(selectedItem.lln)" />
                <text class="limit-label low" x="612" :y="yFor(selectedItem.lln) + 14">下限 {{ selectedItem.lln ?? "--" }}</text>

                <polyline
                  v-if="linePoints"
                  class="trend-area"
                  :points="areaPoints"
                />
                <polyline
                  v-if="linePoints"
                  class="trend-line-svg"
                  :points="linePoints"
                />

                <g v-for="point in plottedPoints" :key="point.label">
                  <circle
                    class="trend-point"
                    :class="{ current: point.current, high: point.direction === 'high', low: point.direction === 'low' }"
                    :cx="point.x"
                    :cy="point.y"
                    r="6"
                  />
                  <text class="point-value" :x="point.x" :y="point.y - 12">{{ formatNumber(point.value) }}</text>
                  <text class="point-label" :x="point.x" y="260">{{ point.label }}</text>
                </g>
              </svg>
            </div>

            <div class="trend-note">
              {{ trendNote }}
            </div>
          </div>
        </section>

        <aside class="panel-right">
          <div class="ai-text-box">
            <strong>{{ selectedItem.item }}</strong>
            <p>{{ summaryText }}</p>
          </div>
          <button class="btn-detail" type="button" @click="$emit('interpret')">
            查看检验报告解读
          </button>
          <div class="arrow-down"></div>
        </aside>
      </div>

      <div v-else class="empty-card">
        这张化验单里没有识别到可排序的异常值。
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  report: {
    type: Object,
    default: null,
  },
});

defineEmits(["interpret"]);

const selectedIndex = ref(0);

const historicalReports = [
  {
    label: "2025年11月",
    note: "较重发热期",
    values: {
      CRP: 42.60,
      WBC: 15.80,
      NEUTP: 0.852,
      LYP: 0.103,
      MOP: 0.042,
      EOP: 0.000,
      BASOP: 0.003,
      NEUTN: 13.46,
      LYN: 1.63,
      MON: 0.66,
      EON: 0.00,
      BASON: 0.05,
      RBC: 4.49,
      HGB: 124,
      HB: 124,
      HCT: 0.391,
      MCHC: 316.0,
      MCH: 27.5,
      MCV: 87.1,
      RDWCV: 0.131,
      RDWSD: 39.5,
      PLT: 274,
      PCT: 0.245,
      MPV: 8.70,
      PDW: 15.60,
    },
  },
  {
    label: "2026年1月",
    note: "轻微感冒期",
    values: {
      CRP: 6.20,
      WBC: 8.70,
      NEUTP: 0.682,
      LYP: 0.228,
      MOP: 0.071,
      EOP: 0.014,
      BASOP: 0.005,
      NEUTN: 5.93,
      LYN: 1.98,
      MON: 0.62,
      EON: 0.12,
      BASON: 0.04,
      RBC: 4.58,
      HGB: 128,
      HB: 128,
      HCT: 0.401,
      MCHC: 321.0,
      MCH: 27.9,
      MCV: 87.6,
      RDWCV: 0.129,
      RDWSD: 39.0,
      PLT: 281,
      PCT: 0.251,
      MPV: 9.10,
      PDW: 15.30,
    },
  },
];

const sortedAbnormalItems = computed(() => {
  const items = props.report?.abnormalItems || [];
  return [...items].sort((left, right) => Number(right.deviationScore || 0) - Number(left.deviationScore || 0));
});

const selectedItem = computed(() => sortedAbnormalItems.value[selectedIndex.value] || sortedAbnormalItems.value[0] || {});

watch(sortedAbnormalItems, () => {
  selectedIndex.value = 0;
});

const selectedKey = computed(() => resolveIndicatorKey(selectedItem.value));

const historySeries = computed(() => {
  const key = selectedKey.value;
  const current = Number(selectedItem.value.numericValue ?? selectedItem.value.value);
  return [
    {
      label: historicalReports[0].label,
      note: historicalReports[0].note,
      value: historicalReports[0].values[key],
    },
    {
      label: historicalReports[1].label,
      note: historicalReports[1].note,
      value: historicalReports[1].values[key],
    },
    {
      label: "本次",
      note: "当前报告",
      value: Number.isFinite(current) ? current : null,
      current: true,
      direction: selectedItem.value.direction,
    },
  ];
});

const chartScale = computed(() => {
  const values = historySeries.value.map((point) => Number(point.value)).filter(Number.isFinite);
  const lln = Number(selectedItem.value.lln);
  const uln = Number(selectedItem.value.uln);
  if (Number.isFinite(lln)) values.push(lln);
  if (Number.isFinite(uln)) values.push(uln);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const span = Math.max(maxValue - minValue, 1);
  return {
    min: minValue - span * 0.18,
    max: maxValue + span * 0.18,
  };
});

const plottedPoints = computed(() => {
  const xValues = [92, 320, 548];
  return historySeries.value
    .map((point, index) => ({
      ...point,
      x: xValues[index],
      y: yFor(point.value),
    }))
    .filter((point) => Number.isFinite(Number(point.value)));
});

const linePoints = computed(() => plottedPoints.value.map((point) => `${point.x},${point.y}`).join(" "));
const areaPoints = computed(() => {
  if (!plottedPoints.value.length) return "";
  const first = plottedPoints.value[0];
  const last = plottedPoints.value[plottedPoints.value.length - 1];
  return `${first.x},236 ${linePoints.value} ${last.x},236`;
});

const gridYs = [56, 101, 146, 191, 236];

const trendNote = computed(() => {
  const first = historySeries.value[0]?.value;
  const second = historySeries.value[1]?.value;
  const current = historySeries.value[2]?.value;
  if (![first, second, current].every((value) => Number.isFinite(Number(value)))) {
    return "该指标暂未匹配到完整历史数据，仅展示能匹配到的时间点。";
  }
  const unit = selectedItem.value.unit ? ` ${selectedItem.value.unit}` : "";
  return `历史对比：2025年11月为 ${formatNumber(first)}${unit}，2026年1月为 ${formatNumber(second)}${unit}，本次为 ${formatNumber(current)}${unit}。`;
});

const summaryText = computed(() => {
  const item = selectedItem.value;
  const range = item.normalRange || item.referenceRange || "未识别";
  return `本次 ${item.value || "--"}${item.unit ? ` ${item.unit}` : ""}，参考范围 ${range}。中间图已加入 2025年11月和2026年1月两组历史数据，便于比较本次异常相对前两次的变化。`;
});

function yFor(rawValue) {
  const value = Number(rawValue);
  if (!Number.isFinite(value)) return 236;
  const { min, max } = chartScale.value;
  if (max <= min) return 146;
  return 236 - ((value - min) / (max - min)) * 180;
}

function resolveIndicatorKey(item) {
  const text = `${item.item || ""} ${item.name || ""}`.toUpperCase().replace(/\s+/g, "");
  if (text.includes("CRP") || text.includes("C反应蛋白")) return "CRP";
  if (text.includes("WBC") || text.includes("白细胞计数")) return "WBC";
  if (text.includes("RDW-SD") || text.includes("RDWSD") || (text.includes("RBC分布宽度") && text.includes("SD"))) return "RDWSD";
  if (text.includes("RDW-CV") || text.includes("RDWCV") || (text.includes("RBC分布宽度") && text.includes("CV"))) return "RDWCV";
  if (text.includes("NEUT")) return text.includes("比例") || text.includes("%") ? "NEUTP" : "NEUTN";
  if (text.includes("LY")) return text.includes("比例") || text.includes("%") ? "LYP" : "LYN";
  if (text.includes("MO")) return text.includes("比例") || text.includes("%") ? "MOP" : "MON";
  if (text.includes("EO")) return text.includes("比例") || text.includes("%") ? "EOP" : "EON";
  if (text.includes("BASO")) return text.includes("比例") || text.includes("%") ? "BASOP" : "BASON";
  if (text.includes("RBC") || text.includes("红细胞计数")) return "RBC";
  if (text.includes("HGB") || text.includes("HB") || text.includes("血红蛋白")) return "HGB";
  if (text.includes("HCT") || text.includes("红细胞比积")) return "HCT";
  if (text.includes("MCHC")) return "MCHC";
  if (text.includes("MCH")) return "MCH";
  if (text.includes("MCV")) return "MCV";
  if (text.includes("PLT") || text.includes("血小板计数")) return "PLT";
  if (text.includes("PCT") || text.includes("血小板压积")) return "PCT";
  if (text.includes("MPV")) return "MPV";
  if (text.includes("PDW")) return "PDW";
  return text;
}

function valueLabel(point) {
  if (!point || !Number.isFinite(Number(point.value))) return "--";
  return `${formatNumber(point.value)}${selectedItem.value.unit ? ` ${selectedItem.value.unit}` : ""}`;
}

function formatNumber(value) {
  if (!Number.isFinite(Number(value))) return "--";
  const abs = Math.abs(Number(value));
  if (abs >= 100) return Number(value).toFixed(0);
  if (abs >= 10) return Number(value).toFixed(2);
  if (abs >= 1) return Number(value).toFixed(2);
  return Number(value).toFixed(3);
}

function shortStatus(direction) {
  if (direction === "high") return "偏高";
  if (direction === "low") return "偏低";
  return "正常";
}

function statusClass(direction) {
  if (direction === "high") return "status-high";
  if (direction === "low") return "status-low";
  return "status-normal";
}

function scoreLabel(score) {
  if (score == null || Number.isNaN(Number(score))) return "--";
  return Number(score).toFixed(3);
}
</script>

<style scoped>
.dashboard-shell {
  padding: 18px 24px;
  background: #f4f5f9;
  border-bottom: 1px solid #e4e7ed;
}

.dashboard-container {
  max-width: 1440px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 15px 25px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.subtitle,
.stat-label,
.range-text,
.trend-summary span,
.trend-summary small {
  color: #909399;
  font-size: 12px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: #303133;
}

.header-stats {
  display: flex;
  gap: 15px;
}

.stat-box {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 6px 20px;
  text-align: center;
  min-width: 70px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.stat-value.alert {
  color: #d14b3f;
}

.main-layout {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(430px, 1.55fr) minmax(220px, 0.8fr);
  gap: 20px;
  height: 520px;
  align-items: stretch;
}

.panel-left,
.panel-center {
  border: 1.5px solid #b3aef5;
  border-radius: 8px;
  padding: 15px;
  min-width: 0;
  height: 100%;
}

.panel-left {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-center {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 10px 15px;
  height: 100%;
  overflow: hidden;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 10px 0;
  color: #303133;
}

.marker-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
  min-height: 0;
  overscroll-behavior: contain;
}

.marker-list::-webkit-scrollbar {
  width: 6px;
}

.marker-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 999px;
}

.marker-item {
  width: 100%;
  display: block;
  padding: 12px;
  border: 1px solid #ebeef5;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  background: #fafafa;
  text-align: left;
}

.marker-item:hover {
  background: #f0f2f5;
}

.marker-item.active {
  border-color: #8f82f1;
  background-color: #f7f6ff;
  box-shadow: 0 2px 6px rgba(143, 130, 241, 0.15);
}

.marker-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 4px;
}

.marker-name {
  font-weight: 700;
  font-size: 13px;
  color: #303133;
  min-width: 0;
}

.marker-status {
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
}

.status-high {
  color: #d14b3f;
}

.status-low {
  color: #2276d2;
}

.status-normal {
  color: #606266;
}

.marker-value {
  display: block;
  font-size: 12px;
  color: #606266;
}

.range-text {
  display: block;
  margin-top: 2px;
}

.trend-card {
  flex: 1;
  min-height: 0;
  padding: 16px;
  background: linear-gradient(180deg, #f7f6ff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.trend-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 10px;
}

.trend-summary div {
  padding: 9px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
}

.trend-summary strong {
  display: block;
  margin: 4px 0;
  color: #303133;
  font-size: 18px;
}

.trend-summary .current strong {
  color: #d14b3f;
}

.chart-box {
  height: 260px;
  overflow: hidden;
}

.chart-box svg {
  width: 100%;
  height: 100%;
}

.grid-lines line {
  stroke: #ebeef5;
  stroke-dasharray: 3 5;
}

.limit {
  stroke-dasharray: 5 4;
  stroke-width: 1.4;
}

.limit.high {
  stroke: #d14b3f;
}

.limit.low {
  stroke: #2276d2;
}

.limit-label {
  font-size: 12px;
  dominant-baseline: middle;
}

.limit-label.high {
  fill: #d14b3f;
}

.limit-label.low {
  fill: #2276d2;
}

.trend-area {
  fill: url("#trendFill");
  stroke: none;
}

.trend-line-svg {
  fill: none;
  stroke: #8f82f1;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-point {
  fill: #8f82f1;
  stroke: #fff;
  stroke-width: 3;
}

.trend-point.current,
.trend-point.high {
  fill: #d14b3f;
}

.trend-point.low {
  fill: #2276d2;
}

.point-value,
.point-label {
  font-size: 12px;
  text-anchor: middle;
  fill: #606266;
}

.trend-note {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  margin-top: 4px;
}

.ai-text-box {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  margin-bottom: 24px;
  max-height: 230px;
  overflow-y: auto;
}

.ai-text-box p {
  margin: 8px 0 0;
}

.btn-detail {
  background: #fff;
  border: 1px solid #409eff;
  color: #409eff;
  padding: 8px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  text-align: center;
  align-self: center;
  transition: all 0.2s;
}

.btn-detail:hover {
  background: #409eff;
  color: #fff;
}

.arrow-down {
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid #409eff;
  margin: 4px auto 0;
}

.empty-card {
  padding: 18px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  color: #606266;
  background: #fafafa;
}

@media (max-width: 980px) {
  .main-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .panel-left {
    height: 420px;
  }

  .panel-center,
  .panel-right {
    height: auto;
  }

  .btn-detail {
    align-self: stretch;
  }
}

@media (max-width: 680px) {
  .dashboard-shell {
    padding: 12px;
  }

  .dashboard-container {
    padding: 14px;
  }

  .header-section,
  .header-stats,
  .trend-summary {
    display: block;
  }

  .stat-box,
  .trend-summary div {
    margin-top: 10px;
  }
}
</style>
