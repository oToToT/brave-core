/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

// MOCK DATA
const topMovers = [ 'BTC', 'ETH', 'CRO' ]

import * as React from 'react'

import createWidget from '../widget/index'
import { StyledTitleTab } from '../widgetTitleTab'

import currencyNames from './data'

import {
  ActionAnchor,
  ActionButton,
  AssetIcon,
  AssetIconWrapper,
  BackArrow,
  Box,
  CryptoDotComIcon,
  FlexItem,
  Header,
  List,
  ListItem,
  PlainButton,
  StyledTitle,
  StyledTitleText,
  Text,
  WidgetWrapper
} from './style'
// import {
//   SearchIcon,
//   ShowIcon,
//   HideIcon
// } from '../exchangeWidget/shared-assets'
import CryptoDotComLogo from './assets/cryptoDotCom-logo'
import { CaratLeftIcon } from 'brave-ui/components/icons'

// Utils
import cryptoColors from '../exchangeWidget/colors'
// import { getLocale } from '../../../../common/locale'

interface State {
  selectedView: string
  selectedAsset: string
}

interface TickerPrice {
  price: number
  volume: number
}

interface AssetRanking {
  lastPrice: number
  pair: string
  percentChange: number
}

interface ChartDataPoint {
  c: number
  h: number
  l: number
  o: number
  t: number
  v: number
}

interface Props {
  showContent: boolean
  optInTotal: boolean
  optInBTCPrice: boolean
  tickerPrices: Record<string, TickerPrice>
  losersGainers: Record<string, AssetRanking[]>
  charts: Record<string, ChartDataPoint[]>
  stackPosition: number
  onShowContent: () => void
  onDisableWidget: () => void
  onTotalPriceOptIn: () => void
  onBtcPriceOptIn: () => void
  onSetLosersGainers: () => void
  onSetTickerPrices: (assets: Array<string>) => Promise<void>
  // onSetChartData: (asset: string) => void
}
interface ChartConfig {
  data: Array<any>
  chartHeight: number
  chartWidth: number
}

class CryptoDotCom extends React.PureComponent<Props, State> {
  constructor (props: Props) {
    super(props)
    this.state = {
      selectedView: 'index',
      selectedAsset: ''
    }
  }

  // This is a temporary function only necessary for MVP
  // Merges losers/gainers into one table
  transformLosersGainers = ({ losers = [], gainers = [] }: Record<string, AssetRanking[]>): Record<string, AssetRanking> => {
    const losersGainersMerged = [ ...losers, ...gainers ]
    return losersGainersMerged.reduce((mergedTable: object, asset: AssetRanking) => {
      let { pair: assetName, ...assetRanking } = asset
      assetName = assetName.split('_')[0]

      return {
        ...mergedTable,
        [assetName]: assetRanking
      }
    }, {})
  }

  setSelectedView = (view: string) => {
    this.setState({
      selectedView: view
    })
  }

  setSelectedAsset = (asset: string) => {
    this.setState({
      selectedAsset: asset
    })
  }

  handleViewMarketsClick = async () => {
    await this.props.onSetTickerPrices(topMovers)
    this.props.onSetLosersGainers()
    this.setSelectedView('topMovers')
  }

  plotData ({ data, chartHeight, chartWidth }: ChartConfig) {
    const dataPoints = data.map((day: any) => day.c)
    const chartArea = chartHeight - 2
    const max = Math.max(...dataPoints)
    const min = Math.min(...dataPoints)
    const pixelsPerPoint = (max - min) / chartArea
    return dataPoints
      .map((v, i) => {
        const y = (v - min) / pixelsPerPoint
        const x = i * (chartWidth / 6)
        return `${x},${chartArea - y}`
      })
      .join('\n')
  }

  renderIconAsset = (key: string, isDetail: boolean = false) => {
    const iconBgColor = cryptoColors[key] || '#fff'

    return (
      <AssetIconWrapper style={{ background: iconBgColor, color: '#000' }}>
        <AssetIcon
          isDetail={isDetail}
          className={`crypto-icon icon-${key}`}
        />
      </AssetIconWrapper>
    )
  }

  renderIndexView () {
    const { optInBTCPrice, onBtcPriceOptIn } = this.props
    const currency = 'BTC'
    const { price = null } = this.props.tickerPrices[currency] || {}

    const losersGainers = this.transformLosersGainers(this.props.losersGainers || {})
    const { percentChange = null } = losersGainers[currency] || {}
    return (
      <>
        <Box isFlex={true}>
          <FlexItem style={{ paddingLeft: 5, paddingRight: 5 }}>
            {this.renderIconAsset(currency.toLowerCase())}
          </FlexItem>
          <FlexItem>
              <Text>{currency}</Text>
              <Text small={true} $color='light'>{currencyNames[currency]}</Text>
          </FlexItem>
          <FlexItem textAlign='right' flex={1}>
            {optInBTCPrice ? (
              <>
                {(price !== null) && <Text>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'USD', currencyDisplay: 'narrowSymbol' }).format(price)}</Text>}
                {(percentChange !== null) && <Text $color='green'>{percentChange}%</Text>}
              </>
            ) : (
              <PlainButton onClick={onBtcPriceOptIn} textColor='green' inline={true}>Show Price</PlainButton>
            )}
          </FlexItem>
          <FlexItem style={{ paddingLeft: 5 }}>
            <ActionButton small={true} light={true}>BUY</ActionButton>
          </FlexItem>
        </Box>
        <Text center={true} style={{ padding: '1em 0 0.5em', fontSize: 15 }}>Get 2% bonus on deposits</Text>
        <Text center={true} style={{ fontSize: 15 }}>Stop paying trading fees</Text>
        <ActionAnchor href='#' style={{ margin: '1em 0' }}>Buy Bitcoin Now</ActionAnchor>
        <PlainButton textColor='light' onClick={this.handleViewMarketsClick} style={{ margin: '0 auto' }}>View Crypto.com Markets</PlainButton>
      </>
    )
  }

  renderTopMoversView () {
    return (
      <List>
        {topMovers.map(currency => {
          const { price = null } = this.props.tickerPrices[currency] || {}
          const losersGainers = this.transformLosersGainers(this.props.losersGainers || {})
          const { percentChange = null } = losersGainers[currency] || {}
          return (
            <ListItem key={currency} isFlex={true} onClick={this.setSelectedAsset.bind(this, currency)}>
              <FlexItem style={{ paddingLeft: 5, paddingRight: 5 }}>
                {this.renderIconAsset(currency.toLowerCase())}
              </FlexItem>
              <FlexItem>
                <Text>{currency}</Text>
                <Text small={true} $color='light'>{currencyNames[currency]}</Text>
              </FlexItem>
              <FlexItem textAlign='right' flex={1}>
                {(price !== null) && <Text>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'USD', currencyDisplay: 'narrowSymbol' }).format(price)}</Text>}
                {(percentChange !== null) && <Text $color={percentChange > 0 ? 'green' : 'red'}>{percentChange}%</Text>}
              </FlexItem>
            </ListItem>
          )
        })}
      </List>
    )
  }

  renderAssetDetailView () {
    const { selectedAsset: currency } = this.state
    const { price = null, volume = null } = this.props.tickerPrices[currency] || {}

    const losersGainers = this.transformLosersGainers(this.props.losersGainers || {})
    const { percentChange = null } = losersGainers[currency] || {}

    const chartData = this.props.charts[currency] || []
    const chartHeight = 100
    const chartWidth = 309
    return (
      <Box noPadding={true}>
        <FlexItem
          hasPadding={true}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            width: '100%',
            borderBottom: '1px solid #979797'
          }}
        >
          <FlexItem>
            <BackArrow>
              <CaratLeftIcon onClick={this.setSelectedAsset.bind(this, '')} />
            </BackArrow>
          </FlexItem>
          <FlexItem style={{ paddingRight: 5 }}>
            {this.renderIconAsset(currency.toLowerCase())}
          </FlexItem>
          <FlexItem flex={1}>
            <Text>{currency}</Text>
            <Text small={true} $color='light'>
              {currencyNames[currency]}
            </Text>
          </FlexItem>
          <FlexItem style={{ paddingLeft: 5 }}>
            <ActionButton small={true} light={true}>
              BUY
            </ActionButton>
          </FlexItem>
        </FlexItem>
        <FlexItem
          hasPadding={true}
          style={{
            width: '100%',
            borderBottom: '1px solid #979797'
          }}
        >
          {(price !== null) && <Text
            inline={true}
            large={true}
            weight={500}
            style={{ marginRight: '0.5rem' }}
          >
            {new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'USD', currencyDisplay: 'narrowSymbol' }).format(price)} USDT
          </Text>}
          {(percentChange !== null) && <Text inline={true} $color='green'>{percentChange}%</Text>}
          <svg
            viewBox={`0 0 ${chartWidth} ${chartHeight}`}
            style={{ margin: '1rem 0' }}
          >
            <polyline
              fill='none'
              stroke='#44B0FF'
              stroke-width='1'
              points={this.plotData({
                data: chartData,
                chartHeight,
                chartWidth
              })}
            />
          </svg>
        <Text small={true} $color='xlight'>7d Graph</Text>
        </FlexItem>
        <FlexItem
          hasPadding={true}
          style={{
            width: '100%'
          }}
        >
          <div style={{ marginTop: '0.2em' }}>
            <Text small={true} $color='light' style={{ paddingBottom: '0.2rem' }}>
              24HR VOLUME
            </Text>
            {(volume != null) && <Text weight={500}>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'USD', currencyDisplay: 'narrowSymbol' }).format(volume)} USDT</Text>}
          </div>
          <div style={{ marginTop: '1em' }}>
            <Text small={true} $color='light' style={{ paddingBottom: '0.2rem' }}>SUPPORTED PAIRS</Text>
            <ActionButton small={true} light={true} inline={true} style={{ marginRight: 5 }}>BTC/CRO</ActionButton>
            <ActionButton small={true} light={true} inline={true}>BTC/USDT</ActionButton>
          </div>
        </FlexItem>
      </Box>
    )
  }

  renderTitle () {
    const { showContent } = this.props
    return (
      <Header showContent={showContent}>
        <StyledTitle>
          <CryptoDotComIcon>
            <CryptoDotComLogo />
          </CryptoDotComIcon>
          <StyledTitleText>
            {'crypto.com'}
          </StyledTitleText>
        </StyledTitle>
      </Header>
    )
  }

  renderTitleTab () {
    const { onShowContent, stackPosition } = this.props

    return (
      <StyledTitleTab onClick={onShowContent} stackPosition={stackPosition}>
        {this.renderTitle()}
      </StyledTitleTab>
    )
  }

  renderSelectedView () {
    const { selectedView } = this.state
    const { selectedAsset } = this.state

    if (selectedAsset) {
      return this.renderAssetDetailView()
    }

    switch (selectedView) {
      case 'topMovers':
        return this.renderTopMoversView()
      default:
        return this.renderIndexView()
    }
  }

  render () {
    const { showContent } = this.props

    if (!showContent) {
      return this.renderTitleTab()
    }

    return (
      <WidgetWrapper tabIndex={0}>
        {this.renderTitle()}
        {this.renderSelectedView()}
      </WidgetWrapper>
    )
  }
}

export const CryptoDotComWidget = createWidget(CryptoDotCom)
