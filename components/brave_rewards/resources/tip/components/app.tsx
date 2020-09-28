/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import * as React from 'react'

import { HostError } from '../lib/interfaces'
import { HostContext } from '../lib/host_context'
import { Locale, LocaleContext } from '../lib/locale_context'
import { injectThemeVariables } from '../lib/theme_loader'

import { PublisherBanner } from './publisher_banner'
import { TipForm } from './tip_form'
import { CloseIcon } from './icons/close_icon'

import * as style from './app.style'

function getErrorDisplay (locale: Locale, error: HostError) {
  return (
    <style.error>
      {locale.getString('errorHasOccurred')}
      <style.errorDetails>
        {error.type} {error.code ? `(${error.code})` : ''}
      </style.errorDetails>
    </style.error>
  )
}

export function App () {
  const host = React.useContext(HostContext)
  const locale = React.useContext(LocaleContext)
  const [hostError, setHostError] = React.useState(host.state.hostError)

  React.useEffect(() => {
    return host.addListener((state) => {
      setHostError(state.hostError)
    })
  })

  function onMount (element: HTMLElement | null) {
    if (element) {
      injectThemeVariables(element)
    }
  }

  return (
    <div ref={onMount}>
      <style.root>
        <style.banner>
          <PublisherBanner />
        </style.banner>
        <style.form>
          <style.close>
            <button onClick={host.closeDialog}><CloseIcon /></button>
          </style.close>
          {hostError ? getErrorDisplay(locale, hostError) : <TipForm />}
        </style.form>
      </style.root>
    </div>
  )
}
