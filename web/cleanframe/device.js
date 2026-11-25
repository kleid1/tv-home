/* CleanFrame's device layer for Android TV, in place of the Tizen one (tv-home).
   Same window.FrameDevice functions as the original, each answered by the app's AndroidTV bridge, plus the TV's
   profile (window.TVProfile) and the Headphones panel's calls. */
(function () {
  'use strict';
  var tv = window.AndroidTV;
  var profile = {};
  try { profile = tv ? JSON.parse(tv.profile()) : {}; } catch (e) { profile = {}; }
  /* Home apps as CleanFrame lists them: id (its mark in assets/icons), name, package. */
  window.TVProfile = {
    apps: (profile.apps || []).map(function (a) { return { id: a.id, name: a.name, mark: true, appId: a['package'] }; }),
    clock: profile.clock || { hours: 24, date: true },
    font: profile.font || null
  };
  var BRANDS = {};
  window.TVProfile.apps.forEach(function (a) { BRANDS[a.appId] = a.id; });

  function unavailable(done) {
    done({ message: 'Open CleanFrame on the TV to use this control.', detail: 'The web preview cannot control the TV.' });
  }
  /* Bridge calls return "" on success or a short message. */
  function call(name, args, done) {
    if (!tv) { unavailable(done); return; }
    var err = tv[name].apply(tv, args);
    done(err ? { message: err } : null);
  }
  function list(name, done) {
    if (!tv) { unavailable(done); return; }
    try { done(null, JSON.parse(tv[name]())); } catch (e) { done({ message: 'The TV could not list these.' }); }
  }
  function apps(done) {
    list('apps', function (err, entries) {
      if (err) { done(err); return; }
      done(null, entries.map(function (a) { return { appId: a.id, name: a.name, brand: BRANDS[a.id] || '', icon: a.icon || '' }; }));
    });
  }
  function sources(done) {
    list('sources', function (err, inputs) {
      if (err) { done(err); return; }
      done(null, inputs.map(function (s) { return { number: s.number, name: s.name, connected: !!s.connected, active: false, source: s.id }; }));
    });
  }
  function setSource(port, done) {
    sources(function (err, inputs) {
      if (err) { done(err); return; }
      var input = null;
      inputs.some(function (s) { if (s.number === port) { input = s; return true; } return false; });
      if (!input) { done({ message: 'This HDMI input is no longer available.' }); return; }
      call('openInput', [input.source], function (e) { done(e, e ? undefined : input); });
    });
  }
  window.FrameDevice = {
    launchApp: function (id, done) { call('launch', [id], done); },
    apps: apps, sources: sources, setSource: setSource, cancelSource: function () {},
    settings: function (done) { call('settings', [], done); },
    network: function (done) { call('network', [], done); },
    headphones: function (done) { list('headphones', done); },
    connectHeadphones: function (address, connect, done) { call('connectHeadphones', [address, connect], done); },
    forgetHeadphones: function (address, done) { call('forgetHeadphones', [address], done); },
    pairHeadphones: function (done) { call('pairHeadphones', [], done); },
    waitForMe: function () { return tv ? tv.waitForMe() : true; },
    setWaitForMe: function (on) { if (tv) tv.setWaitForMe(on); }
  };
}());
