/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import styled from 'styled-components'

import background1 from '../assets/background_1.svg'
import background2 from '../assets/background_2.svg'
import background3 from '../assets/background_3.svg'
import background4 from '../assets/background_4.svg'
import background5 from '../assets/background_5.svg'

export const loading = styled.div``

export const root = styled.div`
  display: flex;
  justify-content: center;
  padding: 64px 12px 10px;
  height: 100%;

  color: #fff;

  background-color: #212529;
  background-size: cover;
  background-position: center;
  background-image: url('${background1}');

  &.background-style-2 {
    background-image: url('${background2}');
  }

  &.background-style-3 {
    background-image: url('${background3}');
  }

  &.background-style-4 {
    background-image: url('${background4}');
  }

  &.background-style-5 {
    background-image: url('${background5}');
  }
`

export const card = styled.div`
  flex: 0 1 700px;
  padding: 32px 27px;
`

export const header = styled.div`
  display: flex;
  align-items: center;
`

export const logo = styled.div`
  flex: 0 0 106px;
  position: relative;
`

export const verifiedIcon = styled.div`
  position: absolute;
  top: 3px;
  left: -12px;

  color: #AEB1C2;

  &.verified {
    color: var(--brave-color-brandBatInteracting);
  }

  .icon {
    width: 32px;
  }
`

export const logoMask = styled.div`
  height: 82px;
  width: 82px;
  text-align: center;
  overflow: hidden;

  background: #fff;
  border-radius: 50%;
  text-align: center;
  color: var(--brave-color-brandBrave);
  text-transform: uppercase;
  font-size: 52px;
  font-weight: 600;
  line-height: 82px;

  > img {
    object-fit: cover;
    max-height: 100%;
  }
`

export const name = styled.div`
  flex: 1 1 auto;

  font-size: 22px;
  line-height: 33px;
  font-weight: 600;
`

export const socialName = styled.div`
  font-weight: normal;
  font-size: 16px;
  line-height: 24px;
  opacity: .75;
`

export const socialLinks = styled.div`
  margin-top: -16px;
  min-height: 25px;
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;

  a {
    display: block;
    width: 25px;
    height: 25px;
    padding: 4px;
    margin: 0 5px;

    border-radius: 50%;
    background: #fff;
  }
`

export const unverifiedNotice = styled.div`
  padding: 7px 14px;
  margin-top: 15px;
  margin-bottom: -12px;
  display: flex;
  align-items: center;

  font-size: 12px;
  background: #fff;
  color: #000;
  border-radius: 8px;
`

export const unverifiedNoticeIcon = styled.div`
  flex: 0 0 32px;
  margin-left: -4px;
  margin-right: 10px;
  height: 32px;
  color: #5DB5FC;
`

export const unverifiedNoticeText = styled.div`
  flex: 1 1 auto;

  a {
    font-weight: 600;
    color: #72b4f6;
  }
`

export const title = styled.div`
  margin-top: 28px;

  font-size: 18px;
  line-height: 26px;
`

export const description = styled.div`
  margin-top: 5px;
`
