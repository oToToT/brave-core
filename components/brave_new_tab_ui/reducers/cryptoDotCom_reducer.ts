/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import { Reducer } from 'redux'
import { types } from '../constants/cryptoDotCom_types'

const cryptoDotComReducer: Reducer<NewTab.State | undefined> = (state: NewTab.State, action) => {
  const payload = action.payload

  switch (action.type) {
    case types.ON_TOTAL_PRICE_OPT_IN:
      state = { ...state }
      state.cryptoDotComState.optInTotal = true
      break

    case types.ON_BTC_PRICE_OPT_IN:
      state = { ...state }
      state.cryptoDotComState.optInBTCPrice = true
      break

    case types.SET_TICKER_PRICES:
      state = { ...state }
      state.cryptoDotComState.tickerPrices = {
        ...state.cryptoDotComState.tickerPrices,
        ...payload
      }
      break

    case types.SET_LOSERS_GAINERS:
      state = { ...state }
      state.cryptoDotComState.losersGainers = payload
      break

    default:
      break
  }

  return state
}

export default cryptoDotComReducer
