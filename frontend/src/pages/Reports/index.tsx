import { useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  useTheme,
  Card,
  CardContent,
  Stack,
  Button,
  IconButton,
  Tooltip,
  LinearProgress,
  Divider,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import DownloadIcon from '@mui/icons-material/Download';
import ShareIcon from '@mui/icons-material/Share';
import PrintIcon from '@mui/icons-material/Print';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
  BarChart,
  Bar,
} from 'recharts';

const StatsCard = styled(Card)(({ theme }) => ({
  height: '100%',
  transition: 'transform 0.3s, box-shadow 0.3s',
  background: 'linear-gradient(135deg, #ffffff 0%, #f8f9ff 100%)',
  border: '1px solid rgba(255, 255, 255, 0.8)',
  '&:hover': {
    transform: 'translateY(-4px)',
    boxShadow: '0 8px 32px rgba(77, 101, 217, 0.1)',
  },
}));

const ActionButton = styled(Button)(({ theme }) => ({
  background: 'linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)',
  color: 'white',
  '&:hover': {
    background: 'linear-gradient(135deg, #3665d5 0%, #7d43d8 100%)',
  },
}));

const ReportsPage = () => {
  const theme = useTheme();
  const [timeRange, setTimeRange] = useState('month');
  const [loading, setLoading] = useState(false);

  // Enhanced mock data
  const stats = {
    totalIncome: 5200,
    totalExpenses: 3800,
    savings: 1400,
    savingsRate: ((1400 / 5200) * 100).toFixed(1),
    topCategories: [
      { name: 'Housing', amount: 1500, color: '#0088FE' },
      { name: 'Food', amount: 800, color: '#00C49F' },
      { name: 'Transportation', amount: 400, color: '#FFBB28' },
      { name: 'Entertainment', amount: 300, color: '#FF8042' },
      { name: 'Utilities', amount: 300, color: '#8884d8' },
    ],
    monthlyTrend: [
      { month: 'Jan', income: 4800, expenses: 3500 },
      { month: 'Feb', income: 5000, expenses: 3600 },
      { month: 'Mar', income: 5200, expenses: 3800 },
      { month: 'Apr', income: 5100, expenses: 3700 },
      { month: 'May', income: 5300, expenses: 3900 },
    ],
    categorySpending: [
      { category: 'Housing', amount: 1500, percentage: 25, color: '#0088FE' },
      { category: 'Food', amount: 800, percentage: 20, color: '#00C49F' },
      { category: 'Transportation', amount: 400, percentage: 15, color: '#FFBB28' },
      { category: 'Entertainment', amount: 300, percentage: 12, color: '#FF8042' },
      { category: 'Utilities', amount: 300, percentage: 10, color: '#8884d8' },
      { category: 'Shopping', amount: 250, percentage: 8, color: '#82ca9d' },
      { category: 'Healthcare', amount: 200, percentage: 6, color: '#ffc658' },
      { category: 'Others', amount: 150, percentage: 4, color: '#a4de6c' },
    ],
  };

  const handleExport = () => {
    setLoading(true);
    // Simulating export delay
    setTimeout(() => setLoading(false), 1500);
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header Section */}
      <Box
        sx={{
          mb: 4,
          background: 'linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)',
          borderRadius: 3,
          p: 3,
          color: 'white',
          boxShadow: '0 8px 32px rgba(77, 101, 217, 0.1)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Financial Reports
          </Typography>
          <Typography variant="subtitle1">
            Comprehensive overview of your financial activities
          </Typography>
        </Box>
        <Stack direction="row" spacing={2}>
          <FormControl sx={{ minWidth: 200, bgcolor: 'rgba(255, 255, 255, 0.1)', borderRadius: 1 }}>
            <InputLabel sx={{ color: 'white' }}>Time Range</InputLabel>
            <Select
              value={timeRange}
              label="Time Range"
              onChange={(e) => setTimeRange(e.target.value)}
              sx={{ color: 'white', '& .MuiSvgIcon-root': { color: 'white' } }}
            >
              <MenuItem value="week">Last Week</MenuItem>
              <MenuItem value="month">Last Month</MenuItem>
              <MenuItem value="quarter">Last Quarter</MenuItem>
              <MenuItem value="year">Last Year</MenuItem>
            </Select>
          </FormControl>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Download Report">
              <IconButton sx={{ color: 'white' }} onClick={handleExport}>
                <DownloadIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Share Report">
              <IconButton sx={{ color: 'white' }}>
                <ShareIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Print Report">
              <IconButton sx={{ color: 'white' }}>
                <PrintIcon />
              </IconButton>
            </Tooltip>
          </Stack>
        </Stack>
      </Box>

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={4}>
          <StatsCard>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6" color="text.secondary">
                  Total Income
                </Typography>
                <TrendingUpIcon sx={{ color: 'success.main' }} />
              </Stack>
              <Typography variant="h4" sx={{ mb: 1 }}>
                ₹{stats.totalIncome.toLocaleString()}
              </Typography>
              <Typography variant="body2" color="success.main">
                +8.2% from last period
              </Typography>
            </CardContent>
          </StatsCard>
        </Grid>
        <Grid item xs={12} md={4}>
          <StatsCard>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6" color="text.secondary">
                  Total Expenses
                </Typography>
                <TrendingDownIcon sx={{ color: 'error.main' }} />
              </Stack>
              <Typography variant="h4" sx={{ mb: 1 }}>
                ₹{stats.totalExpenses.toLocaleString()}
              </Typography>
              <Typography variant="body2" color="error.main">
                +5.4% from last period
              </Typography>
            </CardContent>
          </StatsCard>
        </Grid>
        <Grid item xs={12} md={4}>
          <StatsCard>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6" color="text.secondary">
                  Net Savings
                </Typography>
                <AccountBalanceIcon sx={{ color: 'primary.main' }} />
              </Stack>
              <Typography variant="h4" sx={{ mb: 1 }}>
                ₹{stats.savings.toLocaleString()}
              </Typography>
              <Typography variant="body2" color="primary.main">
                {stats.savingsRate}% savings rate
              </Typography>
            </CardContent>
          </StatsCard>
        </Grid>
      </Grid>

      {/* Charts Section */}
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card sx={{ height: '100%', p: 2 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Income vs Expenses Trend</Typography>
            <ResponsiveContainer width="100%" height={350}>
              <AreaChart data={stats.monthlyTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <RechartsTooltip />
                <Legend />
                <Area
                  type="monotone"
                  dataKey="income"
                  name="Income"
                  stackId="1"
                  stroke="#4776E6"
                  fill="#4776E6"
                  fillOpacity={0.2}
                />
                <Area
                  type="monotone"
                  dataKey="expenses"
                  name="Expenses"
                  stackId="2"
                  stroke="#FF8042"
                  fill="#FF8042"
                  fillOpacity={0.2}
                />
              </AreaChart>
            </ResponsiveContainer>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Card sx={{ height: '100%', p: 2 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Expense Distribution</Typography>
            <ResponsiveContainer width="100%" height={350}>
              <PieChart>
                <Pie
                  data={stats.categorySpending}
                  dataKey="amount"
                  nameKey="category"
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  label
                >
                  {stats.categorySpending.map((entry, index) => (
                    <Cell key={`cell-₹{index}`} fill={entry.color} />
                  ))}
                </Pie>
                <RechartsTooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Grid>

        {/* Spending by Category Section */}
        <Grid item xs={12}>
          <Card sx={{ p: 2 }}>
            <Typography variant="h6" sx={{ mb: 3 }}>Spending by Category</Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={stats.categorySpending}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="category" />
                    <YAxis />
                    <RechartsTooltip />
                    <Bar dataKey="amount" fill="#8884d8">
                      {stats.categorySpending.map((entry, index) => (
                        <Cell key={`cell-₹{index}`} fill={entry.color} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </Grid>
              <Grid item xs={12} md={6}>
                <Box sx={{ maxHeight: 300, overflowY: 'auto' }}>
                  {stats.categorySpending.map((category) => (
                    <Box
                      key={category.category}
                      sx={{
                        mb: 2,
                        p: 2,
                        borderRadius: 1,
                        bgcolor: 'background.paper',
                        boxShadow: '0 0 10px rgba(0,0,0,0.05)',
                      }}
                    >
                      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Box
                            sx={{
                              width: 12,
                              height: 12,
                              borderRadius: '50%',
                              bgcolor: category.color,
                            }}
                          />
                          <Typography variant="subtitle2">{category.category}</Typography>
                        </Stack>
                        <Typography variant="subtitle2" fontWeight="bold">
                          ₹{category.amount}
                        </Typography>
                      </Stack>
                      <Box sx={{ mt: 1, position: 'relative', height: 6, bgcolor: 'grey.100', borderRadius: 3 }}>
                        <Box
                          sx={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            height: '100%',
                            width: `₹{category.percentage}%`,
                            bgcolor: category.color,
                            borderRadius: 3,
                          }}
                        />
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                        {category.percentage}% of total expenses
                      </Typography>
                    </Box>
                  ))}
                </Box>
              </Grid>
            </Grid>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportsPage; 