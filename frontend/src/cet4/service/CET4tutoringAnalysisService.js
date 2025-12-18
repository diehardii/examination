import { triggerHomepageAnalysis } from '@/cet4/api/CET4tutoringAnalysis';

export const startHomepageTutoringAnalysis = async () => {
  try {
    await triggerHomepageAnalysis();
  } catch (error) {
    // 静默失败以免影响首页体验，仅在控制台输出供调试
    // eslint-disable-next-line no-console
    console.debug('[TutoringAnalysis] trigger failed:', error?.response?.data || error?.message);
  }
};
